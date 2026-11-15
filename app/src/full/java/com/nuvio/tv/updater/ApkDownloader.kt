package com.nuvio.tv.updater

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class ApkDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private companion object {
        val TRUSTED_DOWNLOAD_HOSTS = setOf(
            "github.com",
            "release-assets.githubusercontent.com",
            "objects.githubusercontent.com"
        )
    }

    suspend fun download(
        url: String,
        destinationFile: File,
        expectedSha256: String,
        expectedSizeBytes: Long?,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit
    ): Result<File> {
        return try {
            destinationFile.parentFile?.mkdirs()
            val partialFile = File(destinationFile.parentFile, "${destinationFile.name}.part")
            partialFile.delete()

            val request = Request.Builder()
                .url(url)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val finalUrl = response.request.url
                require(finalUrl.isHttps && finalUrl.host in TRUSTED_DOWNLOAD_HOSTS) {
                    "APK download redirected to an untrusted host"
                }
                if (!response.isSuccessful) {
                    error("Download failed: HTTP ${response.code}")
                }

                val body = response.body ?: error("Empty download body")
                val total = body.contentLength().takeIf { it > 0 }
                if (expectedSizeBytes != null && total != null && total != expectedSizeBytes) {
                    error("APK size does not match the release metadata")
                }

                body.byteStream().use { input ->
                    FileOutputStream(partialFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        val digest = MessageDigest.getInstance("SHA-256")
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                        output.flush()
                        if (expectedSizeBytes != null && downloaded != expectedSizeBytes) {
                            error("Downloaded APK size does not match the release asset")
                        }
                        val actualSha256 = digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
                        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                            error("Downloaded APK failed SHA-256 verification")
                        }
                    }
                }
            }
            if (destinationFile.exists()) destinationFile.delete()
            check(partialFile.renameTo(destinationFile)) { "Could not finalize downloaded APK" }
            Result.success(destinationFile)
        } catch (error: Exception) {
            File(destinationFile.parentFile, "${destinationFile.name}.part").delete()
            destinationFile.delete()
            if (error is CancellationException) throw error
            Result.failure(error)
        }
    }
}
