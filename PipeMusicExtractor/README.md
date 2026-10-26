# PipeMusic Extractor

`PipeMusicExtractor` is the multi-platform extraction engine used by the
[PipeMusic client](../PipeMusicClient). It is derived from
[PipePipeExtractor](https://github.com/InfinityLoop1308/PipePipeExtractor), which in turn is a
fork of [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor).

The module has no Android UI. It provides service definitions, search handlers, stream
extraction, cookie and request-header handling for services such as YouTube, SoundCloud,
Bandcamp, BiliBili, Niconico, PeerTube and media.ccc.de.

## Relationship

```text
NewPipe  ->  PipePipe  ->  PipeMusic
```

PipeMusic is an independent music-focused fork of PipePipe. It does not claim affiliation with
the PipePipe or NewPipe projects. Upstream copyright notices and the GPL-3.0 license are
retained.

## Build integration

The extractor is consumed as a Gradle composite build by `PipeMusicClient`:

```gradle
includeBuild('../PipeMusicExtractor')
```

Its Maven coordinate in this fork is:

```text
com.github.InfinityLoop1308.PipeMusicExtractor:extractor
```

The Java package namespace remains `org.schabi.newpipe.extractor` for compatibility with the
upstream engine and the client sources.

## License

GPL-3.0. See the repository [LICENSE](../LICENSE).
