package com.nuvio.tv.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.nuvio.tv.BuildConfig
import java.io.File

/** Update handoff uses Android's standard installer, which always requires the user's confirmation. */
object ApkInstaller {
    val supportsDexMetadata: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun canRequestPackageInstalls(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun buildUnknownSourcesSettingsIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            null
        }

    /** Opens the APK in Android's package installer after verifying package, signature, and upgrade version. */
    fun launchSystemInstaller(context: Context, apkFile: File) {
        ApkIntegrityVerifier.verify(context, apkFile, expectedVersionCode = null)
        UpdateRelaunchReceiver.cancelReopen(context)
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, apkFile)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
