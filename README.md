<h1 align="center">Sauce Tracker</h1>

<p align="center">
  A private, local-first Android library for organizing, exploring, and revisiting gallery metadata.
</p>

<p align="center">
  Library&nbsp;&nbsp;•&nbsp;&nbsp;Browser&nbsp;&nbsp;•&nbsp;&nbsp;History&nbsp;&nbsp;•&nbsp;&nbsp;Heatmaps&nbsp;&nbsp;•&nbsp;&nbsp;Backups
</p>

<p align="center">
  <img alt="Release 1.7" src="https://img.shields.io/badge/release-1.7-8f9cff">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3ddc84">
  <img alt="Kotlin and Compose" src="https://img.shields.io/badge/Kotlin-Compose-7f52ff">
  <img alt="Local first" src="https://img.shields.io/badge/data-local--first-4c9aff">
</p>

Sauce Tracker combines a searchable library, an integrated browser, reading history, recommendations, subscriptions, downloads, backups, and interactive tag and entry heatmaps in one on-device app.

> [!IMPORTANT]
> Sauce Tracker can display links and metadata for adult material. It is intended only for adults where such content is legal. The project is independent and is not affiliated with or endorsed by the website it accesses.

## Feature highlights

### Your library, your way

Search across codes, titles, metadata, dates, pages, tags, artists, and groups. Filter, sort, rate, pin, track read status, and move directly between related parts, even when the destination is hidden by the current filter.

### Browse and import in context

Open a library entry in the integrated browser, detect duplicates, update metadata, and return without losing the active browser task or the visible library state.

### Revisit and discover

Reading history, suggestions, subscriptions, artist pages, related entries, and heatmaps make a large library navigable without sending the collection to a cloud service.

### Read in either direction

Downloaded galleries can be read in horizontal or vertical slideshow modes, with separate privacy behavior for the library, browser, and reader surfaces.

> Screenshots will be added only after a separate privacy review and explicit approval.

## What is new in 1.7

Version 1.7 is the first release built on the reorganized Sauce Tracker architecture. The rewrite preserves the existing design and feature set while separating app lifecycle, storage, network, browser, library, heatmap, subscription, backup, and media responsibilities.

- Redesigned Selected Entry details with a compact expandable Details control.
- Added related-library views for Parts, More like this, and Same artist.
- Parts show matching thumbnail cards for the previous and next entry only.
- Related-entry navigation can open a target even when the current search, tag, download, or Read/Unread filter would normally hide it.
- Restored individual related-entry cards without bringing back the oversized section container.
- Added viewport-bounded Entry Heatmap thumbnails with 10%, 25%, 50%, and 100% display zones; every entry remains available as a lightweight point.
- Improved heatmap memory behavior for large libraries and moved expensive work away from unrelated screens.
- Fixed Library restoration after returning from Browser through app switching or the launcher.
- Preserved the active Browser task when reopening Sauce Tracker from the launcher.
- Replaced ambiguous API errors with clearer website, HTTP, and network messages plus bounded retry behavior for temporary failures.
- Added rolling procedural backup history while keeping the thumbnail archive shared.
- Made subscription notifications open the Subscriptions screen, including the app-lock handoff.
- Moved release credentials out of tracked project configuration.
- Added shared media loading, privacy policies, repositories, feature view models, and diagnostics boundaries for safer future development.

See [CHANGELOG.md](CHANGELOG.md) for the complete release summary.

## Features

### Library

- Search across codes, titles, metadata, dates, pages, tags, artists, groups, and other structured fields.
- Filter by tags and Read, Unread, Downloaded, or All entries.
- Sort, rate, pin, mark as read, inspect reading sessions, and browse related entries.
- Standard card layout and configurable pure gallery layout.
- Artist/group pages, history, suggestions, subscriptions, and recent activity.

### Browser and reader

- Open imported entries or searches in the integrated browser.
- Import and update library metadata without leaving the app.
- Duplicate hints, blocked-tag handling, and separate Browser privacy behavior.
- Local downloads, experimental gallery support, and horizontal or vertical slideshow reading.

### Discovery and visualization

- Personalized suggestions with adjustable weighting and safe fallback behavior.
- Tag Heatmap and precalculated Entry Heatmap with pan, zoom, selection, and bounded thumbnail loading.
- Parts, More like this, and Same artist navigation from Selected Entry.

### Privacy, backup, and background work

- App lock and separate Library/Browser incognito policies.
- Manual import/export and procedural rolling backups.
- Optional shared backup thumbnail archive.
- Subscription background refresh with actionable notifications.
- Optional local Desktop Bridge.

## Download and install

Download `Sauce-Tracker-1.7-release.apk` and its checksum from the [latest GitHub Release](../../releases/latest).

Android 8.0 (API 26) or newer is required.

To update an existing compatible installation with ADB:

```powershell
adb install -r Sauce-Tracker-1.7-release.apk
```

Android may require permission to install apps from the file manager or browser used to open the APK. Back up important library data before replacing an older or differently signed build.

## Build from source

Requirements:

- JDK 21 to run Gradle.
- Android SDK Platform 34 and matching build tools.
- An ARM64 Android device or emulator for the current release target.

```powershell
git clone https://github.com/Roinur/Sauce-tracker.git
cd Sauce-tracker
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Release builds read signing values from an ignored `signing.properties` file, Gradle properties, or environment variables. Copy `signing.properties.example` locally and never commit credentials or keystores.

```powershell
.\gradlew.bat :app:assembleRelease
```

The app compiles Kotlin/Java bytecode for Java 17 while Gradle itself is run with JDK 21.

### Private screenshot workflow

The ADB-only [GitHub media mode](docs/GITHUB_MEDIA.md) opens the real app UI against a separate database copy. It can apply the existing privacy mask while preserving either the normal light or dark color scheme. The production library and rolling backups are not modified.

## Project structure

```text
app/             application entry points, navigation, and lifecycle
background/      scheduled subscription work
core/            network, media, preferences, privacy, storage, and diagnostics
data/            database, DAO, repositories, backup, downloads, and remote parsing
feature/         browser, dashboard, library, heatmap, slideshow, settings, and discovery
```

The package root is `com.example.saucetracker`. The modular layout keeps feature-specific state and expensive work scoped to the screen that owns it.

## Data and network behavior

- Library data, settings, heatmap caches, and reading history are stored locally.
- Network access is used when opening the integrated browser, fetching gallery metadata, refreshing subscriptions, or downloading media.
- Website availability and response formats are outside the project's control; 1.7 distinguishes temporary website failures from device-network problems and permanent HTTP responses.

## Release integrity

Official releases should include:

- `Sauce-Tracker-1.7-release.apk`
- `Sauce-Tracker-1.7-release.apk.sha256`
- release notes matching [CHANGELOG.md](CHANGELOG.md)

Verify the checksum before sideloading when the APK was downloaded through a third party.

## Contributing

Issues should include the app version, Android version, exact screen and action, expected result, and whether the app crashed or only displayed an error. Avoid attaching personal library exports, unblurred thumbnails, signing files, or credentials.

## License

No public source-code license has been selected. Until a license is added, copyright remains with the project owner and the source is not granted for redistribution.
