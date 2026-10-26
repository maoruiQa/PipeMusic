# Changelog

All notable changes to PipeMusic are documented here. This file is written in English; see
`README.zh-CN.md` for a Chinese summary.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-26

First release of PipeMusic, a music-first fork of PipePipe.

### Added

- Music-first single-activity Jetpack Compose interface: Home, Search, Library, Playlists,
  Playlist detail and Profile, with a mini player and a full-screen player.
- Audio-only `MusicPlayerService` built on ExoPlayer with `MediaSessionCompat`: background
  playback, media notification, lock-screen and headset controls, audio focus and a sleep timer.
- Playback queue with sequential, repeat-all, repeat-one and shuffle modes, queue editing and
  persistence across restarts.
- Multi-platform search through PipeMusicExtractor, including YouTube, SoundCloud, Bandcamp,
  BiliBili, Niconico, PeerTube and media.ccc.de, with pagination, service switching and link
  resolving.
- Audio download into the app-private library, with progress, retry, failure reporting and FFmpeg
  extraction for muxed streams.
- Local folder import with recursive scanning, private-directory copies, metadata and embedded
  artwork extraction, progress, cancellation and de-duplication.
- Standalone Room database for tracks, playlists, playlist ordering, play history and search
  history; favourites, recently played and recently added.
- Eight preset themes, custom accent colour, light/dark/system/AMOLED modes, Android 12+ dynamic
  colour, artwork tinting and persistent appearance preferences.
- Launcher and external link routing moved to the music experience; legacy video entry points are
  no longer reachable from the UI.

### Changed

- Client and extractor modules renamed to `PipeMusicClient` and `PipeMusicExtractor`, consumed
  through a Gradle composite build.
- Project documentation and store metadata rebranded to PipeMusic.

### Known limitations

- Some services require cookies, a login, a residential IP address or bot verification, so
  playback and download availability varies by network and region.
- Downloaded and imported files live in app-private storage and are removed when the app is
  uninstalled.
- The published build is a debug-signed APK built from source.
