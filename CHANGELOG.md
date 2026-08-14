# Changelog

All notable Sauce Tracker release changes are documented here.

## 1.8.0 - 2026-08-14

### Sauce Finder

- Added a local Sauce Finder that accepts an image from the picker or Android share sheet and matches it against the user's own library.
- Added crop-tolerant perceptual fingerprints for covers and individual pages, with match confidence, page number, and direct entry navigation.
- Kept the index in a separate SQLite database containing hashes and metadata rather than copies of the images.
- Made indexing incremental and resumable: existing `(entry code, page number)` rows are reused and only missing images are processed.
- Replaced serial remote indexing with four bounded, rate-limited workers and a small queue.
- Added index image/entry counts, actual on-device database size, rounded progress, pause/resume controls, and a more balanced dashboard preview.
- Added Sauce Finder as the third page in the Suggested/Random discovery widget.

### Suggested Entries and dashboard discovery

- Reworked suggestion refresh around cached library profiles, gallery metadata, candidate routes, and scored results.
- Preserved the currently visible list while a refresh runs and made cancellation non-fatal when tags or library state change.
- Persisted Suggested Entry result rows and thumbnail URLs so dashboard previews return after process restarts.
- Made a tapped Suggested preview open the Suggested Entries page and scroll to the matching recommendation.
- Redesigned suggestion cards, mode controls, loading indicator, swipe background, corner radii, and privacy overlays to match the rest of the dashboard.
- Improved thumbnail/cache reuse and bounded duplicate work so repeat visits avoid rebuilding the same expensive state.

### Browser, Library, and related discovery

- Added website-provided More like this recommendations between Browser page overview and comments, using gallery titles rather than only codes.
- Fixed Browser comments that repeated the commenter name where the message should appear.
- Fixed affected Browser tags displaying their count twice.
- Obscured Browser detail titles consistently in incognito and GitHub Media Mode.
- Made Parts navigation independent of Library filters while keeping More like this and Same artist aligned with the active Read/Unread context.
- Preserved direct navigation targets hidden by search terms, tags, download state, or read filters without clearing the user's filters.
- Improved cold thumbnail preload and persistent HTTP thumbnail caching to prevent No preview flashes after app updates.

### Heatmap, subscriptions, and state consistency

- Refined Tag and Entry Heatmap framing so the graph reads as part of the page instead of an oversized nested UI box.
- Preserved the cached layout, complete point set, pan/zoom behavior, family outlines, and 10/25/50/100 percent thumbnail zones.
- Centralized library-change propagation across entries, tags, creators, heatmaps, suggestions, subscriptions, ratings, and read state.
- Made tag counts update atomically against the active library filter instead of briefly showing global totals.
- Hid already imported galleries from subscription feeds, badges, and notification totals while retaining complete event history in backup/export paths.
- Rebalanced dashboard discovery controls and kept subscription counts consistent with other dashboard cards.

### Architecture, privacy, and development

- Continued the architecture cleanup by extracting Dashboard domain models, interactions, parsing, entry lists, tags, creators, subscriptions, suggestions, heatmap UI, backup assembly, and download control.
- Split Browser parsing, duplicate detection, detail UI, gallery-list UI, and media components out of the Browser activity.
- Removed an embedded Desktop Bridge TLS asset and kept generated bridge credentials outside tracked source.
- Added privacy-safe GitHub Media Mode using separate data, copied production obfuscation behavior, stronger capture masking, theme selection, and status-bar-free capture tooling.
- Added a repeatable JDK 21 build conveyor for fast, profile, verify, and signed release builds.
- Expanded policy and Sauce Finder matcher regression tests.

## 1.7.0 - 2026-08-10

### Selected Entry and related navigation

- Reworked the Selected Entry implementation into a dedicated feature component while preserving the established visual design.
- Added a compact expandable Details control with a standard chevron cue.
- Added Parts, More like this, and Same artist relationship modes.
- Limited Parts previews to the previous and next entry and gave them the same thumbnail-card treatment as other related entries.
- Kept related sections visually open while restoring a separate surface for each individual entry.
- Made direct related-entry navigation independent of active search, tag, download, and Read/Unread filters without clearing those filters.

### Library and Browser reliability

- Fixed an empty Entries view after opening Browser, switching apps, returning, and closing Browser.
- Preserved Browser/task state when Sauce Tracker is reopened from its launcher icon.
- Separated Browser and Library privacy state so their incognito behaviors remain intentionally different.
- Improved invalid-response, HTTP, website, and offline error messages.
- Added bounded retry handling for temporary website failures while avoiding retries for permanent responses such as 404.

### Heatmap and performance

- Added 10%, 25%, 50%, and 100% centered thumbnail-zone controls for Entry Heatmap.
- Kept the complete graph available as lightweight points while loading thumbnails only in the selected visible zone.
- Prioritized read entries when scheduling heatmap thumbnails.
- Bounded thumbnail work and caching to reduce memory pressure in large libraries.
- Reduced unrelated background work and added a non-debug profile build for realistic performance validation.

### Backup, subscriptions, and security

- Added rolling procedural backups with Current, Previous 1, and Previous 2 snapshots.
- Kept backup thumbnails in one shared archive rather than duplicating them per snapshot.
- Made subscription notifications navigate to Subscriptions after any required app unlock.
- Moved release signing credentials to ignored local properties or environment variables.

### Architecture

- Reorganized the project under `com.example.saucetracker`.
- Split application lifecycle/navigation, database/DAO/repositories, network/media/security/storage, background work, and feature UI into explicit packages.
- Extracted Browser, slideshow, downloads, backup import/export, suggestions, subscriptions, heatmap, library detail, tags, creators, and history responsibilities from the former monolithic activity implementation.
- Added shared policies and regression tests for privacy, retry behavior, relationship modes, heatmap zones, responsive dashboard scaling, and direct navigation.
