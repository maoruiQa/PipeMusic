# PipeMusic Client

This module contains the Android client of **PipeMusic**, a music-first fork of
[PipePipe](https://github.com/InfinityLoop1308/PipePipe).

PipeMusic keeps PipePipe's extraction and download capabilities but exposes a new Compose-based
music experience instead of the legacy video interface. The client uses the sibling
`PipeMusicExtractor` project through a Gradle composite build.

## Relationship

```text
NewPipe  ->  PipePipe  ->  PipeMusic
```

`PipeMusicClient` is derived from the PipePipe Android client. The original package namespace
(`org.schabi.newpipe`) and application ID (`InfinityLoop1309.NewPipeEnhanced`) are retained for
source and update compatibility. They are implementation details, not the product name.

## Layout

- `app/src/main/java/org/schabi/newpipe/music/` - the PipeMusic data, playback, search, import and
  Compose UI implementation.
- `app/src/main/java/org/schabi/newpipe/` - retained upstream Android client code. Legacy video
  entry points are not part of the product UI.
- `app/src/main/res/` - Android resources and the PipeMusic launcher icon.

## Build

Build from this directory:

```bash
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

The sibling extractor checkout must be available at `../PipeMusicExtractor` because
`settings.gradle` includes it as a composite build.

See the [project README](../README.md) for features, requirements and the overall architecture.

## License

GPL-3.0. Upstream copyright notices and history are preserved.
