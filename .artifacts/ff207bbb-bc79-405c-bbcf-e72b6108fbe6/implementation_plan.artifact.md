# Fix Build Errors - Kotlin Version Mismatch

The build is failing because `firebase-auth:24.2.0` was compiled with a newer version of Kotlin than the one used in the project (2.1.20). Specifically, the library has metadata version 2.3.0, while the project expects 2.1.0.

## User Review Required

> [!IMPORTANT]
> Upgrading the Kotlin version to 2.4.20 is a significant jump. While this should resolve the metadata compatibility issue, it may introduce other warnings or require updates to other plugins.

## Proposed Changes

### Build Configuration

#### [MODIFY] [build.gradle.kts](file:///D:/ConnectHub/build.gradle.kts)
- Upgrade Kotlin version from `2.1.20` to `2.4.20`.
- Upgrade Android Gradle Plugin from `8.7.0` to `9.4.1` to ensure compatibility with newer Kotlin versions.

#### [MODIFY] [app/build.gradle.kts](file:///D:/ConnectHub/app/build.gradle.kts)
- Update Firebase BOM from `33.1.2` to `34.19.0`.
- Remove explicit versions for `firebase-auth` and `firebase-firestore` to let the BOM manage them.
- Clean up `-ktx` suffixes where redundant (Firebase now recommends using the main artifacts as they include KTX extensions).

## Verification Plan

### Automated Tests
- Run `:app:compileDebugKotlin` to verify the compilation error is resolved.
- Run a full build with `./gradlew assembleDebug`.

### Manual Verification
- Sync Gradle to ensure all dependencies are correctly resolved.
