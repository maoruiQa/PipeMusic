# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

PipeMusic is a music-focused fork of PipePipe: it keeps PipePipe's extraction/download engine and replaces the video client with an audio-first Jetpack Compose app. Documentation, issues and PRs are written in English (`docs/zh-CN/` translations are secondary).

## Layout and build model

- `PipeMusicClient/` — Android Gradle project (`:app`, `:ffmpeg`). **All Gradle commands run from here.**
- `PipeMusicExtractor/` — extraction engine (`extractor`, `timeago-parser`; services: YouTube, SoundCloud, Bandcamp, Bilibili, NicoNico, PeerTube, media.ccc). Consumed by the client as a Gradle composite build via `includeBuild('../PipeMusicExtractor')` in `PipeMusicClient/settings.gradle`, so the two directories must remain siblings. Extractor edits are picked up by the client build directly — no publishing step.
- `PipeMusicClient/ffmpeg/` wraps a prebuilt `ffmpeg-kit.aar` (rebuilt only via the manual `build-ffmpeg.yml` workflow).
- `fastlane/` holds store metadata and changelogs; `docs/` holds ARCHITECTURE, BUILD, RELEASE, FAQ, FEATURES.

Toolchain: JDK 25, AGP 9.2.1, Kotlin 2.3.21, compileSdk 37, Gradle wrapper 9.5.1. Android SDK location comes from `PipeMusicClient/local.properties` (`sdk.dir=...`, never commit it).

## Commands (from `PipeMusicClient/`, use `.\gradlew.bat` on Windows)

```bash
./gradlew :app:assembleDebug                 # APKs per ABI + universal in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest             # JVM unit tests
./gradlew :app:testDebugUnitTest --tests "org.schabi.newpipe.music.PlaybackQueueTest"   # single test class
./gradlew :app:lintDebug
./gradlew :app:installDebug
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug   # pre-PR check
```

CI (`.github/workflows/ci.yml`) runs `./gradlew assembleDebug lintDebug --stacktrace -DskipFormatKtlint` on JDK 25. Release builds are signed only when `KEY_PATH`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` are all set; otherwise the APK is unsigned (see `docs/RELEASE.md`).

## Architecture

The upstream NewPipe/PipePipe client code under `org.schabi.newpipe` is retained but is no longer the entry point. **New music code goes in `org.schabi.newpipe.music`** (`PipeMusicClient/app/src/main/java/org/schabi/newpipe/music/`) unless it truly belongs to upstream code. The package namespace and application ID (`InfinityLoop1309.NewPipeEnhanced`, `.debug` suffix for debug) are intentionally unchanged for compatibility and the update channel.

Key flow: Compose UI + `MusicViewModel` → `MusicRepository` (data) and `MusicPlayerController` (playback) → `MusicPlayerService` (ExoPlayer + `MediaSessionCompat`) → extractor.

- **Entry point isolation**: `MusicMainActivity` owns the launcher intent filter; legacy `MainActivity` is unexported with no launcher category; `RouterActivity` intercepts `VIEW`/`SEND` intents and forwards URLs to the music UI. The legacy video player service must never be started (it would compete for the media session).
- **Data**: `MusicDatabase` is a standalone Room DB (`music.db`), deliberately separate from the legacy NewPipe database to avoid migrations against upstream entities. `MusicRepository` is the single entry point for library/playlist/favourite/history/download-state queries. `MusicPreferences` (DataStore) stores theme, audio quality, repeat/shuffle and the last queue snapshot.
- **Playback**: `PlaybackQueue` is pure Kotlin (queue, index, shuffle order, repeat mode) and is the unit-tested reference (`PlaybackQueueTest`). `MusicPlayerController` binds the service and exposes `StateFlow`s; position updates are throttled (~500 ms) to avoid recomposing the root layout every frame. Queue is persisted and restored paused after service restart.
- **Streams**: `AudioStreamResolver` picks the highest-bitrate audio stream; muxed-only services are played as audio and, for downloads, audio is extracted with FFmpeg. Expired URLs are re-resolved once, preserving position. `MusicSearchRepository` wraps extractor search (stream items only, paginated, debounced, cancels on service change; pasted links resolved via service-by-URL lookup).
- **Downloads/import**: both are WorkManager foreground workers. `AudioDownloadWorker` re-resolves inside the worker, writes a temp file then atomically renames; progress/status are written to the `Track` row so the UI just observes Room. `LocalImportWorker` scans an SAF tree, copies into private `files/music`, reads metadata/artwork via `MediaMetadataRetriever`, de-dupes by source URI. Deletes must validate canonical paths inside the app-private `music`/`artwork` dirs.
- **UI**: single activity, Compose; navigation is a sealed route + manual back stack + `BackHandler` (not Navigation Compose). `MusicTheme`/`ColorSchemeFactory` handle seed colours, dynamic colour, AMOLED and artwork tinting, cached per seed and mode.

## Conventions

- Prefer pure Kotlin logic with unit tests over logic inside Composables; keep state hoisted and side effects out of composable bodies.
- Match the formatting of the file being edited; don't reformat unrelated code.
- Logging uses tags `MusicPlayer`, `MusicSearch`, `MusicImport`, `MusicDownload` and records only service IDs, track IDs and error types — never cookies, signed stream URLs or user file paths.
- Strings: add/change in English `values/strings.xml` first; in translations change only text, keeping entry order and placeholders (`%s`, `%1$s`, escaped newlines) intact (see `TRANSLATION.md`).
- Commit subjects are short, imperative, conventional-style (e.g. `fix: resume playback after re-resolve`).
