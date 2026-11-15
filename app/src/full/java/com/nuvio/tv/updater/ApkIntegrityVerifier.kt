package com.nuvio.tv.updater

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Validates the APK identity against this installation before it reaches Android's package installer. */
internal object ApkIntegrityVerifier {
    @Suppress("DEPRECATION")
    fun verify(context: Context, apkFile: File, expectedVersionCode: Long?) {
        check(apkFile.isFile && apkFile.length() > 0L) { "Downloaded APK is missing or empty" }
        val manager = context.packageManager
        val archiveFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val apk = manager.getPackageArchiveInfo(apkFile.absolutePath, archiveFlags)
            ?: error("Downloaded file is not a readable APK")
        check(apk.packageName == context.packageName) { "Downloaded APK belongs to a different app" }
        val installed = manager.getPackageInfo(context.packageName, archiveFlags)
        val apkVersionCode = apk.longVersionCodeCompat()
        check(apkVersionCode > installed.longVersionCodeCompat()) {
            "Downloaded APK is not newer than the installed app"
        }
        if (expectedVersionCode != null) {
            check(apkVersionCode == expectedVersionCode) { "APK versionCode does not match the release" }
        }

        val apkSigners = apk.signerCertificateDigests()
        val trustedInstalledSigners = installed.signerCertificateDigests()
        check(apkSigners.isNotEmpty() && trustedInstalledSigners.isNotEmpty() &&
            apkSigners.any { it in trustedInstalledSigners }) {
            "Downloaded APK signing certificate does not match this installation"
        }
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun PackageInfo.signerCertificateDigests(): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners?.toList().orEmpty() +
                signingInfo?.signingCertificateHistory?.toList().orEmpty()
        } else {
            signatures?.toList().orEmpty()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(Locale.ROOT, it) }
        }.toSet()
    }
}
