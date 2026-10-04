# Changelog

All notable Sauce Tracker release changes are documented here.

## 2.0.0 - 2026-10-04

Sauce Tracker's largest platform update so far: first-class NHentai and MangaDex,
independent profiles, chapter reading, a rebuilt all-in-one Desktop Bridge and QR
sharing, with the existing polished library and reader reused across providers.
See [the complete 2.0 release overview](docs/releases/2.0.md) for the user-facing tour.

- Fixed the release Heatmap Overview ANR on large NHentai libraries: indexed tag-catalog matching replaces quadratic normalization, and catalog preparation runs with graph loading off the UI thread.
- Classified preserved unimported/removed-entry history as healthy "Reading outside library" diagnostics without guessing its origin or discarding sessions; invalid owners and restore mismatches still fail.
- Kept the compact "Saved: Screenshots" link only in Library, where local saved captures belong; removed it from Browser. Centered Library's chapter list on the saved resume chapter in either order, keeping ordinary user scrolling intact.
- Fixed a real 1.9-to-2.0 first-launch crash by initializing pre-migration snapshot tables before SQLiteOpenHelper opens the database; added a constructor-order regression guard.
- Fixed Browser Import/Remove state for MangaDex language entries, added shared chapter-order controls and enabled the existing Library Details disclosure for MangaDex.
- Added a Library entry-filtered Screenshots link into Experimental Gallery, preserving source/language identity on new raw-page captures and matching identifiable legacy captures without changing the stored files.
- Created source-locked NHentai and MangaDex default profiles for fresh installs / 1.9 upgrades; routed pre-2.0 backups and their profile preferences to NHentai while preserving existing debug profiles and separate release storage.
- Deferred Version Museum beyond 2.0; commented out its UI, activity and screenshot packaging while retaining the implementation and manifest for later.
- Added Book Tracker's opt-in Extra dark toggle under Settings / Personalization, sharing its dark surface palette across the app, browser, reader, QR and museum while preserving light/system modes and accent colors.
- Unified source-aware library search/presets, profile-scoped training and recommendation scoring, tag blocking and metadata refresh; removed the local 1,000-entry truncation while retaining bounded database/API pages.
- Prioritized Sauce Finder indexing by downloaded entries, then read entries, then the rest; included MangaDex covers without treating chapters as pages or downloading entire chapters.
- Reused profile taste scoring in Desktop Bridge, preserved tag-block identities across provider catalog merges, and made launch preloading and large-library QR selection source-aware.
- Made backup diagnostics read-only, distinguished preserved browser/removed-entry history from invalid owners, compared legacy history/subscriptions with explicit 2.0 defaults, and rejected incomplete multi-source restores before writes.
- Fixed GitHub media-mode startup with multi-source profiles by copying parent tables first instead of replacing referenced default rows; production data remains read-only during the copy.
- Kept QR generation within both compressed and expanded size limits, rejected duplicate packages before generation, and bounded untrusted preview metadata.
- Added read-only V2 backup preflight for duplicate identities, broken parent relations, invalid title JSON and reader progress; shared export/restore columns and retained compatibility with earlier V5 completion timestamps.
- Fixed selected-profile restore scope so history and subscriptions in a different active profile are not cleared.
- Preserved MangaDex resume and per-chapter progress during profile copy/move, and transactionally removed deleted profiles' history and subscriptions without deleting shared metadata.
- Hardened Desktop Bridge reader cancellation and profile ownership, discarded stale private responses on background/disconnection, and reused bounded in-memory prefetched pages without permanent browser caching.
- Added isolated migration and Bridge regressions to the verify/release conveyor and prevented simultaneous compiler memory exhaustion during full verification.
- Preserved quoted and multiline legacy alternate titles during migration and repaired malformed title JSON in existing 2.0 development databases.
- Revalidated pre-migration safety snapshots against every preserved 1.9 table and checked migrated tags, creators, sessions and subscriptions before committing the upgrade.
- Refined Profiles in Settings into a compact active-profile row and focused manage, edit, create and transfer views, using the app's existing surfaces and privacy styling.
- Added Desktop Bridge dashboard counts, library shortcuts and recent reading using existing profile-scoped APIs.
- Applied Bridge library state filters and sorting before pagination so matches beyond the first page are included.
- Strengthened migration validation for preserved read/rating/pin/dates and membership; expanded Verified Restore's second-pass fingerprint to include source-platform data and all subscription relations, with SQLite integrity validation.
- Fixed Desktop Bridge dashboard navigation and queued view changes during initial sync; kept 2.0 rolling backups as coherent current snapshots, validated their source payloads, supported nonnumeric MangaDex session identities on restore, and separated current-data from existing-backup diagnostics.

- Restyled QR Share and Version Museum with the existing Experimental Gallery theme and chrome, app accent colors, rounded surfaces, browser thumbnails, searchable QR selection and on-demand museum image previews.
- Bridge now preserves source identities for the phone's MangaDex recommendations, keeps History layout when opening details, resets chapter selection between entries, counts distinct chapters in Trends, and checkpoints reader sessions with stable identities and idle-aware reading time.
- Added on-demand Bridge tag/entry heatmaps and raw/rated frequency views using the phone's existing graph computation.

### Sources and profiles

- Added a versioned source-adapter contract and official NHentai and MangaDex adapters with source-aware string identities, capability declarations, normalized search fields, pagination and isolated provider errors.
- Added source-locked and combined profiles, locked NHentai/MangaDex defaults for upgrades and fresh installs, profile-local entry state, and transactional copy/move merge rules. Mixed-profile source scope uses Source tags rather than dashboard chips.
- Moved profile management from the dashboard front into polished Settings surfaces and added quick profile switching by holding the Settings gear.
- Added Search Everything and tag filtering to profile copy/move, and represented temporary source filtering as pinned Source tags only when a profile contains multiple sources.
- Made the existing polished gallery browser source-aware through provider translation: NHentai keeps its established path, MangaDex API data feeds the same browser UI and library actions, and mixed profiles detect known IDs/URLs or ask which source to use.
- Restyled the mixed-source browser chooser to use Sauce Tracker's own popup language.
- Unified MangaDex and NHentai inside the existing Entries list, gallery cards and selected-entry detail instead of exposing a separate profile-library panel or popup.
- Fixed MangaDex covers by using provider-correct image request headers, added latest-readable-chapter delivery through MangaDex@Home into the existing slideshow, and kept long MangaDex UUIDs behind an explicit Copy ID action instead of displaying them throughout the UI.
- Added compact MangaDex chapter lists with cached counts and volume covers to Library and Browser, seamless forward/back chapter continuation, manual series-level rating, and profile-specific resume of the last chapter and page.
- Made MangaDex Browser `Done` close directly without the NHentai end-of-gallery rating prompt; MangaDex series ratings remain manual through the entry stars.
- Added a compact MangaDex title long-press menu for staged SFW, Suggestive and NSFW Browser scopes, and made a new manga's large thumbnail start at its earliest chapter unless saved reading progress exists.
- Kept each imported MangaDex language as a separate entry, added language selection during import, and exposed language as ordinary searchable metadata.
- Connected MangaDex tags, artists, chapters, pages, reading time and chapter progress to the shared History, Trends, Heatmap and Suggested Entries data paths.
- Added profile/source-aware MangaDex subscriptions and corrected mixed-source subscription totals so equivalent terms are not counted twice.
- Fixed MangaDex author subscriptions such as `TYPE-MOON` by preserving punctuation and translating author routes correctly for each provider.
- Made recommendation and preview navigation retain source identity and scroll to the selected MangaDex entry instead of falling back to NHentai.
- Added explicit SQLite schema version 2 migration, a validated pre-migration safety snapshot, integrity checks, and no destructive fallback.

### Reader and reliability

- Added screenshot detection that offers a maximum-quality raw page in Experimental Gallery while keeping the ordinary screenshot, plus a compact screenshot target in the slideshow hold chooser.
- Added per-chapter read-progress shimmer, source-aware reading sessions and completion accounting that records a completed chapter once instead of recounting chapter-boundary navigation.
- Grouped MangaDex History rows by day and series so chapter, page and reading-time totals appear in one card instead of one row per page session.
- Improved MangaDex API reliability by reusing cached chapter metadata, caching short-lived reader manifests, retrying alternate scanlations only after a real failure, and removing redundant preflight image probes.
- Improved MangaDex reader speed and memory safety with reader-optimized images, original-quality raw export fallback, streaming bitmap decode and a byte-bounded page cache.
- Reused one MangaDex adapter and connection pool across Browser, Library, Bridge and Slideshow, and parallelized independent landing-feed requests.
- Made MangaDex slideshow open before network resolution, added cached Browser-first paint, bounded page read-ahead, duplicate-request coalescing and a temporary evictable reader HTTP cache distinct from chapter downloads.
- Started the exact selected MangaDex chapter manifest at tap time and changed reader warmup to priority-first concurrent decode of the current and next three pages through the shared single-flight cache.
- Routed MangaDex images through the stable central host before the assigned-node fallback and stopped adjacent chapters from competing with the selected chapter during first paint.
- Kept a visible page loader between manifest and bitmap display, prioritized the selected MangaDex page alone, then filled a five-page forward read-ahead window through the stable image host.
- Added an evictable MangaDex reader-manifest cache, a dedicated visible-page executor and exact saved-page warming from Browser and Library to reduce repeat chapter launch latency without downloading chapters.
- Released Browser, dashboard and reader bitmap caches on Android memory pressure to reduce MangaDex navigation crashes on 256 MB heaps.
- Routed Browser covers through the shared persistent bounded thumbnail pipeline, coalesced duplicate in-flight cover requests and retained MangaDex's smaller list-cover variant for faster cold and warm Browser rendering.
- Replaced dashboard text glyphs with stronger accent-aware Material You-style Entries, Tags, Artists, Subscriptions, Heatmap and History artwork, and replaced the swipe pin emoji with the app's vector pin while correcting Unpin/Unread gesture contrast.

### Backup, sharing and desktop

- Added the versioned `SAUCE_TRACKER_EXPORT_V2` backup payload with sources, profiles, shared entries, profile state, reading progress and source-aware sessions while retaining V1 import compatibility.
- Extended source backup schema 5 with MangaDex chapter counts, cached chapter metadata and volume covers, and fixed selected-profile restore compatibility with source schemas 2–5.
- Extended Verified Restore with lossless source-platform, session and subscription comparisons plus explicit MangaDex entry, reader-progress, chapter-progress and subscription counts; current data can now be audited even before a rolling-backup folder is configured.
- Added QR Share for one to ten mixed-source entries: bounded/checksummed payloads, QR image generation, Android sharing, camera and saved-image scanning, explicit preview, conflict-safe status handling, and partial import results. Sessions, training, downloads and security state remain excluded.
- Fixed the QR camera startup crash by adding its cache directory to the configured Android `FileProvider` roots.
- Rebuilt Desktop Bridge as static web assets with `/api/v2`, a dark sidebar/library/detail layout based on the 2.0 reference, profile/source-scoped pagination, source-aware mutations, browser-window installation guidance and private DOM clearing on disconnect/background.
- Added an all-encompassing Desktop Browser and slideshow reader with the app's Material You-style icons, internal NHentai/MangaDex discovery, chapter lists, horizontal/vertical reading, adjacent-page prefetch and profile-aware resume, page progress, chapter completion and reading-time history without external redirects.
- Polished Desktop Bridge cards and scrolling, replaced desktop-only import/filter controls with Search Everything import and a compact profile menu, synchronized all mobile accent modes, restored the app artwork, and added an in-reader chapter drawer.
- Synced Bridge AUTO accent to the phone's resolved Material You wallpaper palette, proxied reliable entry/volume thumbnails, added reader zoom and horizontal panning, restored NHentai's Done-rating flow, and surfaced browser reading time/page totals in History and Trends.
- Refined Desktop Bridge with an explicit accent menu, working mixed-source browser scope, bounded pagination, grouped History, visual profile-scoped Trends/Heatmap, provider-aware suggestions, discreet reader chrome, circular navigation and wheel-based horizontal paging.
- Let Desktop Bridge lists, browser results and analytics use the full workspace width until an entry detail is actually opened.
- Restored NHentai discovery and detail loading in Desktop Bridge through NHentai's working v2 API, while retaining the legacy browser path as a fallback.
- Implemented an offline Version Museum timeline, then deferred it beyond 2.0; its UI and packaging remain disabled, with source retained for a later release.

### Compatibility

- Kept package identity `com.roinur.saucetracker`, Android 8.0/API 26 minimum support and the complete NHentai reader/download/browser behavior.
- Integrated the verified MangaDex reader and subscription flows while keeping MangaDex downloads hidden until their complete offline lifecycle is implemented.
- Bounded and downsampled remote thumbnails before they enter Compose and reduced startup image concurrency to prevent image-heavy mixed-source screens from exhausting the Android heap.
- V2 backups are not guaranteed to restore into Sauce Tracker 1.9.

## 1.9.0 - 2026-08-22

### Reading Trends and history

- Added a full Reading Trends page beside Heatmap Overview with configurable page order, swipe navigation, and a persistent floating page indicator.
- Added Tags and Artists / Groups comparisons across Today, Week, Month, Year, and All Time using adaptive time bins.
- Added Reads and Share scales with All, Positive, and confidence-adjusted average-rating views.
- Added stable comparison colors, restrained curve smoothing, drag inspection, long-press period explanations, and ghosted refresh transitions.
- Added minimum prevalence filters, Include misc, View all, and metric-aware Unique Trends combining core interests, metric standouts, and curve-shape outliers.
- Added sample-aware rating handling, long-range Reads normalization, retained empty-period ratios/averages, and partial-bucket corrections.
- Added a Reading History breakdown separating unique reads from rereads.
- Counted the original read in its original trend period and every reread exactly once in the period when it occurred.

### Recommendations and presets

- Added Train your model as a bounded explanation layer over the existing Suggested Entries engine using ordinary tags, artists, and groups while excluding language, category, and similar generic metadata.
- Added reviewable training history plus Not about metadata and Reason not listed escape choices.
- Loaded complete current tag metadata before showing a training question and made long question lists internally scrollable without hiding actions.
- Added named Tag Presets with Include, Either, and Hide rules, editing, ordering, Search Everything suggestions, and direct application as local-library filters.
- Kept presets and training records separate from imported tags, heatmaps, and trend counts.

### Reliability and diagnostics

- Added Library Health checks covering SQLite integrity, orphaned relations, ratings, reading history, document permissions, caches, Sauce Finder, and new local preference stores.
- Added Verified Restore, which imports the current procedural backup twice into an isolated temporary database and verifies idempotence without touching production data.
- Fixed Browser slideshow ratings so already-read entries default to Re-read and save a separate reread session without replacing the original rating.
- Preserved privacy-safe graph QA by masking sensitive labels while leaving trend geometry visible in GitHub Media Mode.
- Extended GitHub Media Mode masking to Train your model and Tag Presets without obscuring their controls.
- Repaired Desktop Bridge TLS handshakes while preserving per-installation credentials: the bridge now generates a software key and self-signed certificate in private no-backup app storage rather than relying on a shared APK asset or an incompatible Android Keystore server key.

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
- Made statistics ranges, activity heatmaps, read activity, and reading-session day grouping follow the phone's local timezone while keeping stored timestamps in UTC.
- Hid already imported galleries from subscription feeds, badges, and notification totals while retaining complete event history in backup/export paths.
- Rebalanced dashboard discovery controls and kept subscription counts consistent with other dashboard cards.
- Added persistent Personalization controls for the actual modern dashboard pagers: Random, Suggested, and Sauce Finder can be reordered independently from Subscriptions, Heatmap, and History.
- Moved entry-cycle, adaptive Home/Dashboard order, Browser, and default-sort controls directly into the Personalization card, removing its intermediate overlay and duplicate order setting.

### Architecture, privacy, and development

- Changed the production Android package identity to `com.roinur.saucetracker`; earlier package installs remain separate so migration uses Sauce Tracker export/import.
- Added optional, direction-aware Gallery Slideshow navigation: Volume Up moves right in horizontal mode and up in vertical mode; Volume Down moves left or down.
- Made vertical volume-button steps center ordinary pages based on their measured height while keeping the first page top-aligned and the last page bottom-aligned.
- Made a slideshow volume-button action enter immersive mode and consume the hardware event so media volume is not changed while the option is enabled.
- Prevented outside taps from dismissing Browser and local Gallery Slideshow rating prompts while preserving the global back gesture as a Skip action.
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
