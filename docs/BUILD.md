# Build and Development

This document describes how to build PipeMusic from source.

## Requirements

| Tool | Version used by this project |
| --- | --- |
| JDK | 25 |
| Android SDK | compile SDK 37, build tools accepted by AGP 9.2.1 |
| Gradle | 9.5.1 (provided by the wrapper, do not install manually) |
| Android Gradle Plugin | 9.2.1 |
| Kotlin | 2.3.21 |

The project builds on Windows, macOS and Linux. No Android Studio installation is required; the
Gradle wrapper downloads everything else.

## Repository layout

```text
PipeMusic/                 # repository root (this directory)
├── PipeMusicClient/       # Android application
│   ├── app/               # application module
│   ├── ffmpeg/            # FFmpeg integration used for audio extraction
│   └── gradle/
├── PipeMusicExtractor/    # extraction engine, consumed as a composite build
├── fastlane/              # store metadata
└── docs/                  # documentation
```

`PipeMusicClient/settings.gradle` contains `includeBuild('../PipeMusicExtractor')`, so the two
directories must stay siblings. The name of the enclosing checkout directory is irrelevant.

## Configure the Android SDK

If your SDK is not in the default location, create `PipeMusicClient/local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

On Windows, escape the backslashes or use forward slashes:

```properties
sdk.dir=C\:\\Users\\me\\AppData\\Local\\Android\\Sdk
```

`local.properties` is ignored by Git and must never be committed.

If a required platform or build-tools package is missing, install it with the SDK command line
tools instead of lowering `compileSdk`:

```bash
sdkmanager "platforms;android-37" "build-tools;37.0.0"
```

## Build the app

From `PipeMusicClient`:

```bash
./gradlew :app:assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

The build produces one APK per ABI plus a universal APK in
`PipeMusicClient/app/build/outputs/apk/debug/`:

```text
PipeMusic_1.0.0-universal-debug.apk
PipeMusic_1.0.0-arm64-v8a-debug.apk
PipeMusic_1.0.0-armeabi-v7a-debug.apk
PipeMusic_1.0.0-x86-debug.apk
PipeMusic_1.0.0-x86_64-debug.apk
```

Debug builds are signed with the standard Android debug key and can be installed directly. They
use the application ID `InfinityLoop1309.NewPipeEnhanced.debug`.

## Build a release APK

```bash
./gradlew :app:assembleRelease
```

Release builds are only signed when all four signing variables are present in the environment:

```bash
export KEY_PATH=/path/to/keystore.jks
export KEY_STORE_PASSWORD=...
export KEY_ALIAS=...
export KEY_PASSWORD=...
```

Without them, `assembleRelease` still produces an APK but it is **unsigned** and cannot be
installed. See [Release process](RELEASE.md) for the full procedure.

## Run tests and static analysis

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Unit tests live in `PipeMusicClient/app/src/test/`. `PlaybackQueueTest` covers the queue,
the repeat modes and shuffle behaviour.

## Useful Gradle tasks

| Task | Purpose |
| --- | --- |
| `:app:assembleDebug` | Build all debug APK variants |
| `:app:assembleRelease` | Build all release APK variants |
| `:app:installDebug` | Install the debug APK on a connected device or emulator |
| `:app:testDebugUnitTest` | Run JVM unit tests |
| `:app:lintDebug` | Run Android lint |
| `:app:clean` | Delete the module build outputs |

## Troubleshooting

**`SDK location not found`** - create `PipeMusicClient/local.properties` with `sdk.dir`.

**`Unsupported class file major version`** - the JVM is older than Java 25. Point `JAVA_HOME` at a
JDK 25 installation, or let the foojay toolchain resolver download one.

**Dependency resolution fails for `com.github.InfinityLoop1308.PipeMusicExtractor`** - the
composite build did not run. Verify that `../PipeMusicExtractor` exists next to
`PipeMusicClient` and that `settings.gradle` still contains `includeBuild('../PipeMusicExtractor')`.

**`Execution failed for task ':app:mergeDebugResources'`** - usually a malformed string resource.
Run the task with `--stacktrace` to see the offending file.

**Kapt or Room warnings about schema export** - harmless. The Room schema export directory is not
enabled for this project.

**Build is slow the first time** - Gradle downloads the wrapper, the toolchain, Compose, Room and
the extractor dependencies. Subsequent builds are incremental.
