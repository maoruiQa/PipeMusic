# PipeMusic translation guide

Thanks for contributing to PipeMusic translations.

PipeMusic is a music-first fork of PipePipe. The app resources still use the upstream
`org.schabi.newpipe` package namespace and the `InfinityLoop1309.NewPipeEnhanced`
application ID for compatibility with the existing codebase and update channel.

Most translations are inherited from the upstream PipePipe/NewPipe resources. New or
changed strings should be written in English first, then translated into the
language-specific `values-*` directories.

## Editing a translation

1. Find the string resource for your language, for example Japanese:
   `PipeMusicClient/app/src/main/res/values-ja/strings.xml`.
2. Change only the text of the affected entries. Keep the order of entries and do not
   modify formatting placeholders such as `%s`, `%1$s` or escaped newlines.
3. Open a pull request describing the language and the strings changed.

If a language is not maintained in this fork yet, copy the English resource file into a
new `values-<language>` directory and translate the entries. Keep all documentation in
this repository in English; localized app strings belong in the Android resource files.
