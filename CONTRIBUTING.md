# Contributing to PipeMusic

Thanks for your interest in PipeMusic. This repository is an independent, music-focused fork of
[PipePipe](https://github.com/InfinityLoop1308/PipePipe).

All documentation, issues and pull requests are written in **English**. Chinese translations may be
added under `docs/zh-CN/`, but the English text is authoritative.

## Before you start

- Read the [project README](README.md) and the [architecture overview](docs/ARCHITECTURE.md).
- Make sure you can build the app: [Build and development](docs/BUILD.md).
- For string changes, also read the [translation guide](TRANSLATION.md).

## Workflow

1. Fork the repository and create a branch from `main`.
2. Keep changes focused. One logical change per pull request.
3. Build and run the app before opening the pull request:

   ```bash
   ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
   ```

4. Add or update unit tests for logic changes. `PlaybackQueueTest` is the reference example.
5. Update the documentation when behaviour changes.
6. Open the pull request and describe what changed, why, and how you tested it.

## Code style

- Kotlin and Java code follows the existing formatting of the file you edit. Do not reformat
  unrelated code.
- New Android code goes into `org.schabi.newpipe.music` unless it legitimately belongs to the
  upstream client.
- Prefer pure Kotlin logic with unit tests over logic embedded in Composables.
- Keep Compose state hoisted and side effects out of the composable body.
- Never log cookies, signed stream URLs or user file paths.

## Commits

- Use the imperative mood and a short subject, for example `fix: resume playback after re-resolve`.
- Reference the issue number in the body when there is one.
- Do not commit build outputs, `local.properties`, keystores or signing secrets.

## Translations

Translations live in `PipeMusicClient/app/src/main/res/values-*/strings.xml`. See
[TRANSLATION.md](TRANSLATION.md) for the exact procedure.

## Reporting bugs

Include the PipeMusic version, the Android version, the service and the affected track, and the
exact steps to reproduce. Check the [FAQ](docs/FAQ.md) first; many issues are caused by service
side restrictions such as cookies, region locks or bot verification.

## License

By contributing you agree that your contribution is licensed under the GNU General Public License
v3.0, the same licence as the project.
