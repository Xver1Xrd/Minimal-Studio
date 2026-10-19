# Project conventions for automated contributors

- Minimal Studio 1.1.0 is a native, local-only Android app in Kotlin / Jetpack Compose / Room.
- All user-facing strings are Russian. The operating systems used to build it are Linux Fedora / Manjaro.
- Keep the two spaces (`wish`, `idea`) and the top pill-shaped `StudioIsland`.
- The Android manifest must NOT add INTERNET permission or unnecessary broad storage permissions.
- Build with JDK 17, SDK Platform 36, Build Tools 35.0.0, Gradle 8.13, AGP 8.13.2, Compose BOM 2026.06.00.
- Room schema version is 2. A migration from schema 1 adds `deletedAt` and keeps all records. Never use destructive migration fallback.
- Cards are soft-deleted into Trash; permanent deletion and file removal require deliberate user action.
- The editor saves a private text draft and navigates away only after `StudioRepository.save()` succeeds.
- All repository writes and backup operations use a Mutex to avoid cross-operation file/database races.
- Files are copied from Photo Picker / SAF into app-owned storage. FileProvider has scoped read grants.
- Restore ZIP only after bounded extraction/validation; update Room atomically after copying into fresh private filenames.
- Do not commit a release signing key, password, real card data or exported ZIP.
- Before shipping run `scripts/offline-smoke.sh` plus Gradle lint, unit tests, connected Android instrumented tests and manual `docs/RELEASE_CHECKLIST.md`.
- Not yet verified by an Android build or device in the source-authoring environment: do not claim release certification without completing those steps.
