# Frequently Asked Questions

## General

### Is PipeMusic affiliated with PipePipe or NewPipe?

No. PipeMusic is an independent fork of PipePipe, which is itself a fork of NewPipe. Upstream
copyright notices and the GPL-3.0 licence are preserved.

### Why is the application ID still `InfinityLoop1309.NewPipeEnhanced`?

Changing it would break the existing update channel and make already downloaded data invisible to
the app. The ID and the `org.schabi.newpipe` package namespace are compatibility details, not the
product name. The app is labelled **PipeMusic**.

### Why does the release contain a debug APK?

The published build is produced from source with the standard debug signing key so that it is
installable without a private keystore. Functionally it is the same application; only the signing
key and the application ID suffix differ. Build your own signed release if you need a stable
application ID.

## Playback

### A service returns no results or playback fails

Some services require cookies, a login, a residential IP address or bot verification. Availability
therefore depends on your network and region. Try another service, or open the same track through a
different network.

### Playback stops after a while

Stream URLs expire. The app re-resolves the URL once and resumes at the previous position. If the
service keeps rejecting the request, wait and retry, or download the track for offline playback.

### The notification disappeared

Android may kill background services on devices with aggressive battery optimisation. Exempt
PipeMusic from battery optimisation in the system settings.

### Bluetooth or headset buttons do not work

Media buttons require an active media session. Start playback once after installing the app, and
grant the notification permission on Android 13 and above.

## Downloads and imports

### A download fails

Common causes: the service does not expose a downloadable audio stream, the URL expired, or the
request was rate limited. Retry from the downloads sheet. HLS-only sources are handled through
FFmpeg when possible.

### My imported files disappeared

Imported and downloaded files are stored in app-private storage and are removed when the app is
uninstalled, or when you clear them from **Profile > Storage**. Back up anything important.

### Import is slow on a large folder

Scanning is recursive and copies are streamed in 64 KB buffers. Very large folders take time; keep
the app in the foreground until the progress completes, or cancel and import smaller subfolders.

## Appearance

### The theme resets after a restart

Preferences are stored in DataStore. If they reset, the app data was cleared or the device blocked
writes. Reapply the theme once; it should then persist.

### Dynamic colour is not available

Dynamic colour requires Android 12 or newer and a launcher that supports it. The app falls back
silently to the selected preset theme.

## Privacy

### What is logged?

Only service IDs, track IDs and error types, under the tags `MusicPlayer`, `MusicSearch`,
`MusicImport` and `MusicDownload`. Cookies, signed stream URLs and user file paths are never
logged.

### Does the app upload my library?

No. The library, playlists and history stay on the device.

## Building

See [Build and development](BUILD.md). The most common problems are a missing
`local.properties` and a JVM older than Java 25.
