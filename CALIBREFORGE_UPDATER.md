# CalibreForge Android TV updater

The full-flavor in-app updater now reads GitHub releases from `calibrenyc/NuvioTVCalibreForge`. The existing client checks the Stable or Beta release channel, picks a compatible APK asset, downloads newer versions, and installs through Android's package installer.

## Publishing an update

1. Increment both `versionName` and `versionCode` in `app/build.gradle.kts`.
2. Publish a GitHub Release in this fork with the APK asset(s). The repository's `.github/workflows/android-release.yml` builds the full release APKs and creates a release when manually dispatched in `publish` mode. Beta/RC versions are marked prerelease; Stable selects GitHub's latest non-prerelease release.
3. Keep the same release signing certificate for every `fullRelease` build. The app must also retain its package ID (`com.nuvio.tv`) for Android to accept each APK as an in-place update.

The TV app checks at startup when update checking is enabled and schedules background checks about every 12 hours. When it finds a newer release it downloads the APK and displays the update banner. Installing still requires the user unless automatic installation has been enabled and the Android device supports it. Android 12+ and the install-unknown-apps permission are required for the configured idle, no-confirmation install path.

## Important: debug APKs

The APK used for the initial physical-TV acceptance was `fullDebug`. Debug builds use a separate application ID (`com.nuviodebug.com`), suppress automatic updater checks, and the CI build creates a temporary signing key for each run. A `fullRelease` APK from a GitHub Release uses `com.nuvio.tv` and the configured release signing key. Android cannot install that APK over the debug app. Install a signed `fullRelease` build as a separate app to exercise release updates; subsequent release builds can update it when package ID and signing key remain the same. Existing remote account and sync data can be used after signing in to that app.

The fork currently has no published GitHub Releases, so there is not yet a release APK for the app to discover. The debug APK workflow artifact is not a GitHub Release and is not used by the updater.

## Verification status

- Updater repository target changed from the upstream NuvioTV repository to this fork.
- Release discovery, channel selection, APK asset selection, background download, and installation are existing behavior; no interface or backend changes were made.
- The fork currently has no GitHub Releases, and no fullRelease APK has been published. End-to-end update discovery and installation therefore remain unverified until the first signed release is published and installed.
