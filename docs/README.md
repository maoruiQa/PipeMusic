# PipeMusic Documentation

All documentation in this repository is maintained in **English**. Chinese translations are
provided as a convenience under [`docs/zh-CN/`](zh-CN/README.md) and
[`README.zh-CN.md`](../README.zh-CN.md). When the two differ, the English document is authoritative.

## Documents

| Document | Contents |
| --- | --- |
| [Build and development](BUILD.md) | Requirements, SDK setup, Gradle tasks, tests and build troubleshooting |
| [Architecture](ARCHITECTURE.md) | Modules, packages, data layer, playback, workers and UI structure |
| [Features and usage](FEATURES.md) | Screens, playback controls, downloads, folder import, playlists and themes |
| [FAQ](FAQ.md) | Known limitations, playback and download problems, privacy and storage |
| [Release process](RELEASE.md) | Versioning, APK variants, signing and how a release is published |
| [Contributing](../CONTRIBUTING.md) | Workflow, code style, commits and pull requests |
| [Translation guide](../TRANSLATION.md) | How to add or fix app string translations |
| [Changelog](../CHANGELOG.md) | Release history |

## Start here

- New to the project? Read the [project README](../README.md) for the product overview.
- Want to build it? Follow [Build and development](BUILD.md).
- Want to understand the code? Read [Architecture](ARCHITECTURE.md).
- Looking for a download? See the
  [releases page](https://github.com/maoruiQa/PipeMusic/releases).

## Documentation conventions

- File paths are relative to the repository root unless stated otherwise.
- Shell commands use POSIX syntax. On Windows, replace `./gradlew` with `.\gradlew.bat` and run
  the commands from PowerShell.
- Code, identifiers and file names are written in monospace; UI labels are written in bold.
