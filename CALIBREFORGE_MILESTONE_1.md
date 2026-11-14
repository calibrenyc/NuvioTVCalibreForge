# CalibreForge integration: milestone 1

This milestone establishes a reproducible full debug APK build and maps the existing app's account and sync contract. It does not change app behavior, API endpoints, or backend configuration.

## Project and build audit

- Android application modules: `:app`, `:baselineprofile`, and `:ffmpeg-decoder-downmix`.
- Gradle wrapper: 8.13. The Android Gradle Plugin and dependencies are pinned in `gradle/libs.versions.toml` and the Gradle build scripts.
- App requirements: compile/target SDK 36, min SDK 24, Java and Kotlin bytecode target 11, and NDK `29.0.14206865` (required by the native decoder module). The full distribution flavor includes the plugin/runtime integrations excluded from `playstore`.
- Build command: `./gradlew :app:assembleFullDebug` (Windows: `./gradlew.bat :app:assembleFullDebug`). Expected universal APK: `app/build/outputs/apk/full/debug/app-full-universal-debug.apk`.
- Local SDK configuration: install Android SDK platform/build tools 36.0.0 and NDK 29.0.14206865; set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to the SDK directory, or set `sdk.dir` in ignored `local.properties`.
- `local.properties` and `local.dev.properties` are ignored. Debug configuration accepts Supabase and third-party API configuration there; blank values are permitted for a compile-only build. Do not commit production credentials or signing material. CI generates an ephemeral debug keystore on the runner because `fullDebug` uses the release signing config; no app secrets are required.
- The repository is licensed under GPL-3.0 (`LICENSE`). Preserve applicable notices and GPL terms for derivative distribution.

## Authentication contract observed in the Android client

The app currently expects a Supabase-compatible URL and publishable/anon key, configured via `NUVIO_SUPABASE_URL`, `NUVIO_SUPABASE_ANON_KEY`, and optional `NUVIO_SUPABASE_FALLBACK_URL` in local properties. Auth requests send `apikey`, JSON content headers, and a bearer access token where applicable.

Required auth routes include:

- `POST /auth/v1/signup` for email/password registration.
- `POST /auth/v1/token?grant_type=password` for email/password login.
- `POST /auth/v1/token?grant_type=refresh_token` for access-token refresh. Responses must remain compatible with the Supabase token/session shape consumed by the Kotlin Supabase Auth client, including access token, refresh token, expiry, user ID, and email.
- `POST /rest/v1/rpc/start_device_login_session` and `start_tv_login_session` for TV/device login initiation. The client sends device nonce, redirect URL, device type, and optional device name; the response contains device/user code, verification URL(s), expiry, and polling interval.
- `POST /rest/v1/rpc/poll_tv_login_session` with code and device nonce. The response contains the pending/approved/expired state and timing fields.
- `POST /functions/v1/tv-logins-exchange` with code and device nonce; return a token response the app can import as a Supabase session.
- `POST /rest/v1/rpc/get_sync_owner` to resolve the effective sync owner for linked/shared accounts.
- `POST /rest/v1/rpc/register_current_device` after sign-in, with installation ID, client name/version, platform, and device name.

The client also supports provider-specific OAuth/device authorization (Trakt, MDBList, Simkl, debrid services). These are third-party integrations and are not substitutes for the app account contract above. Custom server connections are enabled in the `full` flavor; they configure media/addon servers and do not replace account authentication or Nuvio cloud sync.

## Profile contract

Profiles are stored locally in Android DataStore and synchronized for a signed-in account using authenticated PostgREST RPCs:

- `sync_push_profiles`: accepts `p_client_max_profiles`, `p_profiles` (profile index, name, avatar color, primary addon/plugin flags, avatar ID/URL, background ID/URL), and sync-origin client ID.
- `sync_pull_profiles`: returns rows with profile index, name, avatar color, addon/plugin flags, avatar and background fields.
- `sync_pull_profile_locks`: returns profile index and whether a PIN is enabled.
- `set_profile_pin`, `clear_profile_pin`, and `verify_profile_pin`: set/clear/verify a profile PIN, with current-PIN enforcement and verification lockout metadata (`unlocked`, `retry_after_seconds`).
- `sync_delete_profile_data`: removes one profile's remote data and accepts a profile ID and sync-origin client ID.

The custom backend must preserve stable profile IDs and enforce per-account authorization and PIN checks server-side. Profile settings are separately synchronized through `sync_pull_profile_settings_blob`, `sync_push_profile_settings_blob`, and `sync_copy_profile_setup`.

## Synchronization contract

All listed app sync APIs are authenticated PostgREST RPCs, generally scoped by `p_profile_id`; mutation requests include a stable client ID via sync-origin metadata. The implementation uses refresh-and-retry when a JWT expires. Preserve JSON field names and response shapes from `SupabaseModels.kt` and the relevant service DTOs.

- **Library:** `sync_pull_library`, `sync_get_library_delta_cursor`, `sync_pull_library_delta`, `sync_push_library_items`, `sync_delete_library_items`.
- **Watch progress:** `sync_get_watch_progress_delta_cursor`, `sync_pull_watch_progress_delta`, `sync_pull_watch_progress`, `sync_push_watch_progress`, `sync_delete_watch_progress`.
- **Watched items:** `sync_get_watched_items_delta_cursor`, `sync_pull_watched_items_delta`, `sync_pull_watched_items`, `sync_push_watched_items`, `sync_delete_watched_items`.
- **Profiles/settings:** `sync_push_profiles`, `sync_pull_profiles`, `sync_pull_profile_locks`, `sync_delete_profile_data`, `sync_pull_profile_settings_blob`, `sync_push_profile_settings_blob`, `sync_copy_profile_setup`, plus the PIN RPCs above.
- **Other device state:** `sync_push_addons`, `sync_push_plugins`, `sync_pull_collections`, `sync_push_collections`, `sync_pull_home_catalog_settings`, `sync_push_home_catalog_settings`, `sync_pull_media_servers`, `sync_push_media_servers`, `sync_seed_provider_credentials`, `sync_pull_provider_credentials`, and `sync_push_provider_credentials`.

Watch/library APIs use snapshot and delta cursors with bounded pages/batches. A replacement should retain ordering, deletion tombstones, event IDs/cursors, profile isolation, and origin-client metadata to avoid overwriting newer state or echoing a device's own changes.

Additional read-only/entitlement RPCs used by some screens include `get_my_member_access`, `get_my_membership_overview`, `get_avatar_catalog`, `get_member_profile_avatar_catalog`, and `get_member_profile_background_catalog`. Storage is also used for account-backed avatar/background assets.

These contracts are inferred from the Android client source; this audit does not assert that a custom server implementation already exists or that all optional services are enabled. Verify exact SQL signatures and authorization policies against the deployed backend before implementing compatibility.

## Build attempt and results

- Branch created: `feature/calibreforge-integration` from `dev`.
- Unmodified build command attempted: `./gradlew :app:assembleFullDebug`.
- First attempt stopped during Gradle configuration because no Android SDK path was exported. Retried with `ANDROID_HOME` and `ANDROID_SDK_ROOT` set to the installed SDK.
- The retry stopped because the environment had about 605 MB free and Gradle could not install required NDK `29.0.14206865` (`Not enough space on the disk`). No application code or endpoint changes were made. APK was not produced locally.
- GitHub Actions fallback: `.github/workflows/calibreforge-debug-apk.yml` builds this feature branch and uploads the universal APK as `calibreforge-full-debug-apk`. It uses JDK 17, SDK 36/build-tools 36, NDK 29, and an ephemeral generated debug keystore, with no production secrets. The first push run failed in `setup-android@v3` before Gradle started because its default package list included the removed SDK package `tools` (`Failed to find package 'tools'`). The setup action is now limited to `platform-tools`; the required platform, build-tools, and NDK are installed explicitly by `sdkmanager`.
- Existing `.github/workflows/pr-full-debug-build.yml` also builds and uploads this APK for pull requests targeting `dev`.

## Remaining blockers and milestones

1. **APK milestone:** a successful GitHub Actions build and artifact upload remain pending after correcting the retired SDK package setup failure. The branch push should retrigger the workflow; inspect its result and download/install the artifact on a Google TV or emulator. No local APK is available because the machine cannot install the required NDK with its remaining disk space.
2. **Backend compatibility:** inventory the independently developed backend's auth, profile, and sync APIs against the contracts above. Decide whether to implement Supabase-compatible routes or introduce an explicit client abstraction in a later milestone.
3. **Authentication/profile integration:** agree on account/device linking, token lifecycle, ownership rules, profile identity, PIN throttling, and migration behavior before changing app endpoints.
4. **Data synchronization:** define schemas and merge semantics for library, playback progress, watched items, settings, provider credentials, and conflict/deletion handling; add compatibility tests before enabling the custom backend.
5. **Admin dashboard and rollout:** align entitlements and administrative controls, then stage a reversible opt-in rollout while preserving current Nuvio behavior.
