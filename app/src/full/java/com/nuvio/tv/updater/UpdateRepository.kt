package com.nuvio.tv.updater

import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.GitHubReleaseApi
import com.nuvio.tv.updater.model.AppUpdate
import javax.inject.Inject
import javax.inject.Singleton

internal class NoEligibleUpdateException(channel: UpdateChannel) :
    IllegalStateException("No compatible APK release found for ${channel.storedValue} channel")

@Singleton
class UpdateRepository @Inject constructor(
    private val gitHubReleaseApi: GitHubReleaseApi
) {

    suspend fun getLatestUpdate(): Result<AppUpdate> {
        return runCatching {
            val owner = BuildConfig.GITHUB_OWNER
            val repo = BuildConfig.GITHUB_REPO

            val response = gitHubReleaseApi.getLatestRelease(owner = owner, repo = repo)
            if (!response.isSuccessful) {
                if (response.code() == 404) throw NoEligibleUpdateException(UpdateChannel.STABLE)
                val detail = when (response.code()) {
                    403 -> "GitHub rate limit or access restriction (HTTP 403)"
                    429 -> "GitHub rate limit reached (HTTP 429)"
                    in 500..599 -> "GitHub is temporarily unavailable (HTTP ${response.code()})"
                    else -> "GitHub API error: HTTP ${response.code()}"
                }
                error(detail)
            }
            val releases = listOf(response.body() ?: throw NoEligibleUpdateException(UpdateChannel.STABLE))
            val releaseWithAsset = ReleaseSelector
                .eligibleReleases(releases, UpdateChannel.STABLE)
                .firstNotNullOfOrNull { release ->
                    AbiSelector.chooseBestApkAsset(release.assets)?.let { asset ->
                        release to asset
                    }
                }
                ?: throw NoEligibleUpdateException(UpdateChannel.STABLE)
            val (dto, asset) = releaseWithAsset

            val versionCode = Regex("(?m)^<!--\\s*calibreforge-version-code:\\s*(\\d+)\\s*-->\\s*$")
                .find(dto.body.orEmpty())?.groupValues?.get(1)?.toLongOrNull()
                ?: error("Release is missing CalibreForge versionCode metadata")
            val digest = asset.digest?.removePrefix("sha256:")?.lowercase()
                ?.takeIf { it.matches(Regex("[0-9a-f]{64}")) }
                ?: error("Release APK is missing its GitHub SHA-256 digest")

            val tag = dto.tagName?.takeIf { it.isNotBlank() }
                ?: dto.name?.takeIf { it.isNotBlank() }
                ?: error("Release has no tag/name")

            if (!asset.browserDownloadUrl.startsWith("https://github.com/")) {
                error("Release APK download URL is not on github.com")
            }
            AppUpdate(
                tag = tag,
                title = dto.name?.takeIf { it.isNotBlank() } ?: tag,
                notes = dto.body.orEmpty().replace(
                    Regex("(?m)^<!--\\s*calibreforge-version-code:\\s*\\d+\\s*-->\\s*$"), ""
                ).trim(),
                releaseUrl = dto.htmlUrl,
                assetName = asset.name,
                assetUrl = asset.browserDownloadUrl,
                assetSizeBytes = asset.size,
                versionCode = versionCode,
                sha256 = digest
            )
        }
    }
}
