# CalibreForge Nuvio TV updater

The Android `full` flavor checks GitHub's latest published, non-prerelease release in `calibrenyc/NuvioTVCalibreForge` at startup and through **Settings → Check for updates**. It compares the release's `versionCode` with the installed APK and selects only a signed `app-full-<abi>-release.apk` asset (preferring a matching ABI and otherwise the universal APK). Debug and Play Store APKs, source archives, drafts, and prereleases are excluded.

When an update is available, the TV-friendly dialog shows the version, release notes, and download size. **Update now** downloads with progress, validates the GitHub SHA-256 digest and expected file size, verifies the APK package name, higher `versionCode`, and signing certificate against the installed app, then opens Android's package installer. The user must approve the installation. If Android asks for install permission, enable “Allow from this source” for Nuvio in system settings and retry. No broad storage permission is used. Update prompts are held while the player screen is open.

Updates use Android's normal package update path, which preserves app data, including accounts, settings, and local synchronization state, provided the package ID and signing identity remain unchanged.

## Release and signing setup

The repeatable release workflow is `.github/workflows/android-release.yml` (`Build Android TV Release`). Before the first release:

1. Keep `applicationId = "com.nuvio.tv"` for the `fullRelease` app. The debug app has a separate ID and cannot be upgraded to the release app.
2. Increment both `versionName` and `versionCode` in `app/build.gradle.kts`. The release metadata script requires two distinct version bumps in repository history and rejects a non-increasing version code.
3. Configure repository Actions secrets: `NUVIO_RELEASE_KEYSTORE_BASE64` (the base64 encoded release keystore), `NUVIO_RELEASE_KEY_ALIAS`, `NUVIO_RELEASE_KEY_PASSWORD`, `NUVIO_RELEASE_STORE_PASSWORD`, and `LOCAL_PROPERTIES_BASE64` (base64 encoded `local.properties` with required app service configuration). `LOCAL_DEV_PROPERTIES_BASE64` is optional. Keep these values and the keystore out of Git. Reuse the exact same release keystore for every update; rotating/loss of the key prevents in-place updates.
4. From the version-bump branch, run the workflow in `dry-run` mode to review release notes and checks. Run `publish` to build/sign the full release APKs and publish the stable GitHub Release, or `draft` to create a draft for review.

The workflow builds `fullRelease`, checks APK package/version metadata and signature validity, attaches `app-full-*-release.apk` assets plus `SHA256SUMS.txt`, and includes the release `versionCode` metadata used by the app. It never publishes a debug APK as a stable update. The app uses GitHub's server-computed SHA-256 asset digest and validates it before accepting the download.

## Verification and remaining work

- Verified by source inspection: application ID `com.nuvio.tv`, current version `1.2.0-beta.1` / version code `1078`, release workflow signing via Actions secrets, stable-only release lookup, expected fullRelease asset filtering, SHA-256/size checks, package/signing/version validation, Android installer handoff, Settings manual check, and playback-screen prompt suppression.
- The updater unit-test command could not reach compilation in this environment: Gradle first needed an Android SDK path, and with the already-present SDK path supplied, configuration stopped because the existing NDK folder `29.0.14206865` is incomplete (missing `source.properties`). No NDK was installed or changed.
- No physical Android TV installation or upgrade test was possible here. The fork currently has no GitHub Releases, so version detection, download, package install, account retention, sync retention, cancellation recovery, and downgrade rejection still need verification after the release secrets are configured, a stable signed APK is published, and a TV/device is available.

Install the first signed `fullRelease` APK manually. Subsequent releases with the same package ID and signing key will appear as in-app updates; the debug CI artifact is not eligible for stable update installation.
