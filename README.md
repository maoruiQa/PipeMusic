# PipeMusic

PipeMusic is a music-first Android app built on top of the PipePipe codebase. It keeps the
multi-platform extraction and download engine, but replaces the video-oriented interface with a
focused listening experience: search for music, stream audio, download tracks for offline use,
import a local folder, organize playlists, and personalize the interface with themes.

This repository is the PipeMusic fork. The original PipePipe project remains the upstream
source for the extraction engine and much of the low-level platform integration.

## Relationship to PipePipe and NewPipe

```text
NewPipe  ->  PipePipe  ->  PipeMusic
```

- **NewPipe** is the original libre YouTube/streaming client.
- **PipePipe** is a feature-rich fork of NewPipe. It provides the multi-platform extractor,
  download infrastructure and the legacy Android client that PipeMusic is derived from.
- **PipeMusic** is an independent, music-focused fork of PipePipe. It uses PipePipe's extractor
  and playback/download primitives, while the user-facing Android app is a new Compose-based
  music player. Video playback, subscriptions, comments and channel pages are not exposed in the
  product UI.

PipeMusic is **not affiliated with or endorsed by** the PipePipe or NewPipe projects. Upstream
copyright notices and the GPL-3.0 license are preserved.

English is the primary language of this repository. A Chinese companion document is available at
[README.zh-CN.md](README.zh-CN.md); when the two differ, the English version is authoritative.

### What is reused

- `PipeMusicExtractor`: the PipePipeExtractor engine, including service definitions, search,
  stream extraction, cookies, request headers and platform-specific handling.
- The existing downloader, cookie store, playback data-source and FFmpeg integrations.
- The upstream application namespace and application ID, to keep compatibility with the
  existing codebase and update channel.

### What changed in PipeMusic

- A new single-activity Jetpack Compose interface: Discover, Search, Library, Playlists and
  Profile, plus a mini player and a full-screen player.
- An audio-only `MusicPlayerService` built on ExoPlayer 2.18.7 and `MediaSessionCompat`.
- A separate Room database for tracks, playlists, play history and search history.
- Local folder import with recursive SAF scanning, private-directory copies, metadata and
  embedded artwork extraction, progress, cancellation and deduplication.
- Playlist management, favorites, recently played, recently added, queue editing, sequential,
  repeat-all, repeat-one and shuffle playback.
- Eight preset themes, custom accent color, light/dark/system/AMOLED modes, Android 12+ dynamic
  color, artwork tinting and persistent preferences.
- Background playback, media notification, lock-screen controls, headset controls and a sleep
  timer.

## Download

Prebuilt APKs are published on the
[releases page](https://github.com/maoruiQa/PipeMusic/releases). Pick `universal` if you are not
sure which ABI your device uses, or the `arm64-v8a` build for most modern phones. The published
build is a debug-signed APK built directly from this source tree.

## Features

- Search across the services supported by PipeMusicExtractor, including YouTube, SoundCloud,
  Bandcamp, BiliBili, Niconico, PeerTube and media.ccc.de.
- Stream search results directly and add them to the playback queue.
- Download audio into the app-private library where the source exposes a downloadable audio
  stream. HLS-only sources are handled through FFmpeg when possible.
- Import an entire local folder: files are copied into the app-private `music` directory so they
  remain available without external storage permissions.
- Create, rename and delete playlists; add, remove and reorder tracks.
- Favorite tracks and browse listening history.
- Control playback from the notification, lock screen or a wired/Bluetooth headset.

## Project layout

```text
PipeMusic/                 # repository root; see the note below
├── PipeMusicClient/       # Android app, Compose UI, music services
│   ├── app/
│   └── gradle/
├── PipeMusicExtractor/    # PipePipeExtractor engine (composite build)
├── fastlane/              # Store metadata
└── README.md
```

The module directories in this fork are `PipeMusicClient` and `PipeMusicExtractor`. If you
already have an older clone, use these new directory names (or rename the old directories) before
running the build. The name of the enclosing checkout directory does not matter to Gradle; it can
still be the upstream name `PipePipe` in an existing working copy. A fresh clone can use
`PipeMusic` as the checkout directory name.

The Android client includes the new music implementation in:

```text
PipeMusicClient/app/src/main/java/org/schabi/newpipe/music/
├── data/        # Room database, repository and preferences
├── local/       # folder scanning, metadata and import worker
├── playback/    # queue, service, controller and notification
├── stream/      # search, audio selection and download worker
└── ui/          # Compose theme, screens, components and view model
```

## Documentation

| Document | Contents |
| --- | --- |
| [docs/README.md](docs/README.md) | Documentation index |
| [docs/BUILD.md](docs/BUILD.md) | Requirements, SDK setup, Gradle tasks, tests |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, data layer, playback, workers, UI |
| [docs/FEATURES.md](docs/FEATURES.md) | Screens, playback, downloads, import, themes |
| [docs/FAQ.md](docs/FAQ.md) | Known limitations and troubleshooting |
| [docs/RELEASE.md](docs/RELEASE.md) | Versioning, APK variants and publishing |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Workflow, code style and pull requests |
| [TRANSLATION.md](TRANSLATION.md) | Translating app strings |
| [CHANGELOG.md](CHANGELOG.md) | Release history |

Chinese translations of the guides are available under [docs/zh-CN/](docs/zh-CN/README.md).

## Build

Requirements:

- JDK 25
- Android SDK with compile SDK 37 and build tools accepted by the project
- Windows, macOS or Linux

Create `PipeMusicClient/local.properties` if your SDK is not in the default location:

```properties
sdk.dir=/path/to/Android/Sdk
```

Build the debug APK from the repository root:

```bash
cd PipeMusicClient
./gradlew :app:assembleDebug
```

On Windows:

```powershell
cd PipeMusicClient
.\gradlew.bat :app:assembleDebug
```

The APK is written to `PipeMusicClient/app/build/outputs/apk/debug/`. The ABI-split and universal
debug variants are generated by the project configuration. See [docs/BUILD.md](docs/BUILD.md) for
signing, tests and troubleshooting.

Run the unit tests and lint:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

## Compatibility notes

- The application ID is intentionally still `InfinityLoop1309.NewPipeEnhanced` (with `.debug` for
  debug builds). Changing it would break the existing update channel and installed-data
  compatibility.
- The Java/Kotlin package namespace is still `org.schabi.newpipe` for the same reason. This is a
  source-level compatibility detail, not the product name.
- The Gradle project names and module directories are `PipeMusic`, `PipeMusicClient` and
  `PipeMusicExtractor`. The extractor Maven coordinate is
  `com.github.InfinityLoop1308.PipeMusicExtractor:extractor`.
- The top-level directory name is independent from the Gradle project name. Existing clones may
  still have a `PipePipe` checkout folder; only the module names inside it were changed in this
  rebranding.
- Existing git history and upstream copyright notices are retained. The git submodule names in
  `.gitmodules` may still reference upstream repository names; their checkout paths are updated
  for this fork.

## Known limitations

- Some services require login, cookies, a residential IP address or bot verification. Playback
  and download availability can therefore vary by network and region.
- Downloads are saved in the app's private storage. Uninstalling the app removes them; back up
  your files if needed.
- The application is delivered as a debug APK from source; it is not an official PipePipe or
  NewPipe release.

## License

PipeMusic is distributed under the GNU General Public License v3.0. See [LICENSE](LICENSE).

Upstream projects:

- [PipePipe](https://github.com/InfinityLoop1308/PipePipe)
- [NewPipe](https://github.com/TeamNewPipe/NewPipe)

All project documentation in this repository is maintained in English.
