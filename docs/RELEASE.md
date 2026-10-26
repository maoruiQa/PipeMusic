# Release Process

## 1. Version

The version is defined in `PipeMusicClient/app/build.gradle`:

```gradle
def baseVersionCode = 1
def appVersionName = "1.0.0"
```

Update `baseVersionCode` (monotonically increasing) and `appVersionName`. Each ABI variant
receives its own `versionCode` of `100 * baseVersionCode + abiCode`, so a device always receives a
variant built for it.

## 2. Changelog

Add the release notes to `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. The file
is named after the lowest ABI version code, that is `100 * baseVersionCode + 1`. Keep it short and
user facing. Mirror the same content into the GitHub release body.

## 3. Build

```bash
cd PipeMusicClient
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APKs are written to `PipeMusicClient/app/build/outputs/apk/debug/`:

```text
PipeMusic_1.0.0-universal-debug.apk
PipeMusic_1.0.0-arm64-v8a-debug.apk
PipeMusic_1.0.0-armeabi-v7a-debug.apk
PipeMusic_1.0.0-x86-debug.apk
PipeMusic_1.0.0-x86_64-debug.apk
```

Debug APKs are signed with the debug key and installable. Publish at least the universal APK and
the `arm64-v8a` and `armeabi-v7a` splits; they cover virtually every phone.

## 4. Optional signed release build

Set the signing variables before running `assembleRelease`:

```bash
export KEY_PATH=/path/to/keystore.jks
export KEY_STORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...
```

Without them the release APK is unsigned and cannot be installed. Keep the keystore and the
passwords private.

## 5. Tag and publish

1. Commit the version bump and the changelog.
2. Create an annotated tag matching the version name:

   ```bash
   git tag -a v1.0.0 -m "PipeMusic 1.0.0"
   git push origin v1.0.0
   ```

3. Create the GitHub release for that tag and upload the APKs as assets.
4. Verify that the installed APK starts on a clean device and that the version shown in
   **Profile > About** matches the tag.

## Checklist

- [ ] `baseVersionCode` and `appVersionName` updated
- [ ] Fastlane changelog added
- [ ] Unit tests and lint pass
- [ ] APKs built and signed (debug key or release key)
- [ ] Tag pushed
- [ ] Release published with APK assets
- [ ] Release notes list features, fixes and known limitations
