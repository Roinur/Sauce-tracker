# Changelog

All notable Sauce Tracker release changes are documented here.

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

