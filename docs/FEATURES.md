# Features and Usage

PipeMusic is a music player. Video playback, subscriptions, comments and channel pages are not
part of the product UI.

## Screens

### Home

Shows a greeting, the search entry and the theme shortcut. The main card offers **Continue
listening** with artwork, progress and a play button. Below it: recently played, service shortcuts,
recently added and downloaded music.

### Search

Type a query to search the selected service. Use the service chips to switch between YouTube,
SoundCloud, Bandcamp, BiliBili, Niconico, PeerTube, media.ccc.de and the other services provided by
the extractor. Results are paginated; each row offers download and a context menu. Pasting or
sharing a link into the app resolves the track directly.

### Library

Shows track count, total duration and storage used. **Import folder** opens the system folder
picker and copies every supported audio file (mp3, m4a, aac, flac, wav, ogg, opus) into the private
library, including metadata and embedded artwork. Filters: All, Downloaded, Recently added,
Favourites. Multi-selection enables batch add-to-playlist and delete.

### Playlists

Favourites plus user playlists shown as a mosaic grid. A playlist detail page offers play all,
shuffle, reordering and removal of tracks. Adding a track to a playlist can also create a new
playlist on the spot.

### Profile

Brand header with library statistics, a live theme preview that opens the theme sheet, and
settings for appearance, download quality, storage, supported services, open source licences and
version information.

## Playback

- Mini player above the bottom navigation with artwork, title, play/pause, next and a thin
  progress line. Tap or swipe up to open the full player.
- Full player shows a large artwork, title, artist, a draggable progress bar, shuffle, previous,
  play/pause, next and repeat, plus favourite, add to playlist, download, queue and sleep timer.
- Repeat modes: off (sequential), repeat all, repeat one. Shuffle can be combined with any mode.
- Background playback with a media notification, lock-screen controls and headset buttons.
- Sleep timer stops playback after the selected duration.

## Downloads

Downloads run as foreground work and survive leaving the screen. Progress is shown in the
downloads sheet. When a service exposes a direct audio stream, the original container and codec
are kept; otherwise the audio track is extracted with FFmpeg. Files are stored in the app-private
`files/music` directory and artwork in `files/artwork`.

## Storage and backup

Imported and downloaded files live in app-private storage. They are excluded from Android cloud
backup. **Uninstalling the app removes them**, so export anything you want to keep.

## Appearance

Eight preset themes, a custom accent colour with an HSV picker and hex input, light, dark, system
and AMOLED modes, Android 12+ dynamic colour, and optional artwork-based tinting on the player
screen. All choices persist across restarts.

## Permissions

| Permission | Why |
| --- | --- |
| Internet | Search, streaming and downloads |
| Foreground service / media playback | Background playback and the media notification |
| Notifications (Android 13+) | Playback controls and download progress |
| Post notifications | Download and import progress |
| Storage access framework folder access | Importing an existing local library |

The app never asks for broad read access to shared storage; folder import uses the system picker
and a persisted URI grant.
