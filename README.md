# AppControlX

AppControlX is a native Android app management utility built with Kotlin and Jetpack Compose.

## What It Does

- Freeze/unfreeze apps
- Force stop apps
- Clear app cache/data
- Uninstall apps for current user
- Launch hidden settings and activities
- Monitor CPU, RAM, storage, battery, and device stats
- Keep action history with rollback for supported operations

## Architecture (v4 Native)

- **UI:** Jetpack Compose + Material 3
- **State:** ViewModel + StateFlow
- **DI:** Hilt
- **Persistence:** DataStore + kotlinx.serialization
- **Execution layer:** libsu (root) and Shizuku

No WebView, React, or JavaScript bridge is used in v4.

## Requirements

- Android 10+ (API 29+)
- Android SDK 34
- JDK 17
- Root (Magisk/KernelSU) or Shizuku for privileged actions

## Build

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/`

## Signed Release Build

Release builds require signing environment variables. There is no debug-key fallback for release.

Required variables:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Build command:

```bash
./gradlew assembleRelease
```

Release APK output:

`app/build/outputs/apk/release/`

## CI

- `build.yml` runs lint, unit tests, and debug APK build.
- `release.yml` validates signing secrets, builds signed release APK, uploads artifact, and publishes GitHub release on `v*` tags.

## Permissions and Network Behavior

The app requests `INTERNET` permission in the manifest. Core app-management and monitoring flows are local/on-device and do not rely on a remote backend service.

## License

GPL-3.0
