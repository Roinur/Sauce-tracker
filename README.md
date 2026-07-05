### Sauce Tracker Feature List 

  - Gallery fetch by code: Add/update entries from plain codes (e.g. 177013) and hash-codes (e.g. #177013).
  - Gallery fetch by link: Accept full gallery links (nhentai.net/g/{code}/).
  - Creator fetch by name: Accept artist/group names directly and resolve them.
  - Creator fetch by link: Accept nhentai.net/artist/{name}/ and nhentai.net/group/{name}/.
  - Smart input parsing: Auto-detect whether input is code, gallery link, creator name, or creator link.
  - Single add/update: One-click add/update from the main input bar.
  - Batch add/update: Import .txt with many items at once.
  - Ambiguous two-word prompt: When two words appear together, asks whether to combine as one creator or split entries.
  - Short-code safety prompt: Warns when detected numbers are likely invalid (too short).
  - Split-number safety prompt: Detects split digit patterns and asks combine/skip.
  - Duplicate prevention: Skips already-known entries and creators/groups.
  - Refetch selected entry: Refresh metadata for one selected entry.
  - Delete selected entry: Remove selected entry directly from its expanded card.
  - Pin entries: Pin/unpin entries so pinned items stay at the top.
  - Rating system (0-5): Per-entry star rating with direct star tap behavior.
  - Rating reset: Reset rating back to 0 quickly.
  - Code quick-copy: Tap code in selected entry to copy to clipboard.
  - Inline selected card: Expanded selected entry appears directly under the tapped row.
  - Tap-to-collapse selected: Tapping selected row again collapses it.
  - Thumbnail preview (row): Small thumbnail in compact row layout.
  - Thumbnail preview (expanded): Larger thumbnail in selected entry card.
  - Tap-to-zoom image: Tap expanded image to open larger rounded viewer.
  - Tap image to close: Tap full image again to close (no extra close button).
  - Tags with counts: Tag list shows per-tag usage counts.
  - Multi-tag filtering (AND): Selecting multiple tags narrows entries to matches containing all selected tags.
  - Filter reset: Quick reset for active tag filters.
  - Tag search: Search inside the tag list without dropping selected tag filters.
  - Universal search: One "Search everything" bar across entries, tags, and creators/groups.
  - Field-aware search tokens: Supports targeted queries like artist:, group:, tag:, title:, code:, pages:, upload:,
    fetched:, added at:.
  - Date range search: Added at supports date ranges.
  - Search result counters: "Showing" counters for active search/filter states.
  - Tag sorting: Sort tag list by name/type/count, asc/desc.
  - Creator sorting: Sort artist/group list by name/type/count, asc/desc.
  - Entry sorting: Sort entries by title/pages/upload/added/rating.
  - Single active sort rule: Only the latest chosen sort is active.
  - Sort direction colors: Direction-coded sort chips (descending vs ascending colors).
  - Collapsible sections: Entries, Tags, and Artists/Groups sections can each collapse/expand.
  - Creator cards: Artist/group cards expand to show linked entries.
  - Creator link from entry: Tapping artist/group name in selected entry jumps to that creator section and opens it.
  - Cross-filtered creators: Tag/search filters also affect which creators and creator entries are shown.
  - Import full snapshot: Import full metadata backup from text format.
  - Export full snapshot: Export complete saved metadata (entries, ratings, tags, creators/groups, links).
  - Offline restore: Imported snapshots restore data without refetching website metadata.
  - Export signature header: Export files include a recognizable Sauce export header.
  - Procedural backup: Auto-updating backup file support.
  - Manual backup now: Run backup instantly from settings.
  - Backup folder picker: Choose backup destination folder via Android file picker.
  - Clear-all safety flow: Clear-all asks whether to export first, then warns before destructive clear.
  - Clear-all scope: Clears both entries and creators/groups.
  - Material You theming: Dynamic theme behavior with system colors where available.
  - Theme mode cycling: Light / Dark / Auto toggle icon in top bar.
  - Settings panel: Centralized controls for import/export/backup/clear and options.
  - On-screen stats: Settings shows totals for entries/artists/groups.
  - Back gesture handling (app): Back gesture works through app state transitions, not just immediate exit.
  - Back gesture handling (browser): Back works inside in-app browsing flow.
  - In-app browser launch: Open selected entry directly in internal browser.
  - Input preview browser launch: Use current input (code/link/creator) to preview in browser without adding.
  - Home browser shortcut: Quick open to nhentai.net from main input area.
  - Reader direct-open path: Selected-entry open uses /g/{code}/1/ for direct reader entry.
  - Compact reader mode: Reader-focused layout tuning and crop behavior.
  - Popup/new-window suppression: In-app browser denies popup-window behavior.
  - Ad/tracker hardening: Gecko content-blocking configuration with strict anti-tracking setup.
  - First-launch browser warmup: Browser performs first-use warmup before showing content.
  - Browser readiness gate: "Preparing browser..." overlay prevents early first-load race conditions.

### Sauce Tracker 1.1 - New Features--------------------------------------------------------------------------------------------------------------------------------------

  - Incognito privacy mode: Double-tap the Sauce Tracker title to toggle private mode.
  - Incognito visual theme: Dedicated incognito palette and icon, independent from wallpaper accent colors.
  - Privacy masking layer: Sensitive content (titles, codes, tags, creator names, thumbnails, URLs, metadata) is obfuscated with rounded masks.
  - Incognito safety controls: Open-in-browser and copy/jump actions from sensitive fields are blocked while privacy mode is enabled.
  - Accent color picker: Press-and-hold the theme toggle to open a Material You-style color picker strip.
  - Accent presets: Auto/wallpaper plus fixed red, orange, amber, green, teal, blue, indigo, and pink accent modes.
  - App lock system: Optional lock on app open with local PIN and biometric unlock support.
  - PIN workflow upgrades: Up to 20-digit PIN, keyboard auto-focus on lock screen, Enter/Done to unlock.
  - Lock grace period: App can remain unlocked briefly after background/close (30-second grace window).
  - Search-everything expansion: Unified search now targets entries, tags, and artists/groups together.
  - Advanced search tokens: Fielded queries such as code:, title:, pages:, uploaded:, fetched:, added at:, artist:, group:, tag:, language:, category:, parody:, character:, type:, and rating:.
  - Date-range queries: Added-at filtering supports range-style date search.
  - Live match counters: Search and tag-filter fields show dynamic "Showing: N" feedback while typing/filtering.
  - Browser query builder: Selected tags plus search terms can be combined and launched directly in-browser as one nhentai search query.
  - Search reset ergonomics: Dedicated reset action for the universal search field.
  - Read-state tracking: Entries now support Read/Unread status with color-coded state display.
  - Read filtering: Toggle between Show All, Show Read, and Show Unread in the entries panel.
  - Browser-exit rating flow: On exit from selected-entry browser view, prompt to rate/save or skip; saving marks as read.
  - Stats extension: Settings totals include read count in addition to entries/artists/groups.
  - Creator quick actions: Long-press artist/group name in selected entry to copy to clipboard.
  - Clipboard quick tools: Gallery input now includes direct paste action.
  - Browser ad resilience upgrades: Stronger first-load warmup, repeated guard injection, popup suppression, click interception, and DOM mutation cleanup.
  - Signed 1.1 release: Built as Sauce-Tracker-1.1-release.apk (versionCode 4, versionName 1.1).




### Sauce Tracker 1.2 - New Features--------------------------------------------------------------------------------------------------------------------------------------

  - Browser architecture upgrade: Replaced the old in-app browser flow with a metadata-driven Sauce Browser UI tailored to mobile.
  - Browser home feed split: Clear separation between "Popular now" and "Recently added" sections.
  - Search sort controls in browser: Added `Recent`, `Today`, `Week`, and `All time` sorting for search results.
  - Creator sort controls in browser: Added the same `Recent/Today/Week/All time` modes for artist/group pages.
  - Browser back-stack navigation: Back gesture/button now walks true browser states (entry, creator, search, home) instead of prematurely exiting.
  - Browser detail redesign: Entry metadata in the browser now uses Material-style grouped chips for cleaner scanning.
  - Browser tag popularity labels: Tag chips show popularity values with compact formatting (e.g., `19k`).
  - Browser code quick-copy: Tapping the code in browser detail copies it to clipboard.
  - Browser creator deep-linking: Tapping artist/group in browser detail opens that creator page in the same browser flow.
  - Browser comments support: Gallery comments are fetched and rendered inside browser detail view.
  - Comment author parsing fix: Cleaned malformed author strings so usernames display correctly.
  - Slideshow reader mode: Opening a gallery from browser detail launches a direct image slideshow view.
  - Page-target opening: Tapping a specific page thumbnail opens slideshow on that exact page instead of always page 1.
  - Slideshow touch controls: Added left/right tap zones for previous/next page navigation.
  - Slideshow gesture behavior: Kept swipe navigation and tuned tap-vs-swipe handling for more reliable page turns.
  - Slideshow quick exit: System back gesture/button now returns directly to gallery overview from slideshow.
  - Slideshow theme sync: Slideshow background/colors follow Sauce Tracker theme + accent mode.
  - Browser privacy session hardening: API and image clients now run with `NO_COOKIES`.
  - Browser exit privacy cleanup: On browser close/destroy, clears session pools, cookies, web storage, and in-memory thumbnail cache.
  - Search/input unification: Merged gallery-code and broad search behavior into one primary `Search everything` field.
  - Parser priority rule: Code/hash/link/artist/group parsing is attempted before generic keyword search logic.
  - Direct route detection for single terms: Single recognized terms can open typed pages directly (`/tag/`, `/language/`, `/character/`, `/parody/`, `/category/`, `/artist/`, `/group/`) instead of generic search.
  - Direct route from single tag filter: If exactly one tag filter is selected, browser open targets that tag-type route directly.
  - Direct route URL recognition: Pasted links like `https://nhentai.net/tag/{name}/popular` (and other typed routes) are parsed as direct route targets.
  - Hashtag search parity: `#123456` input works the same as `123456` inside search logic.
  - Expandable search bars: Search and selected-tags fields expand vertically to fit long content.
  - Search UI consistency: Search field now matches the outlined/label style used by tag filter.
  - Search reset action: Added dedicated `Reset search` control beside filter actions.
  - Browser open query upgrades: Open-in-browser now supports combined search terms + selected tags in one generated query.
  - Blocked-tags browser integration: Blocked tags are automatically appended as exclusions in generated browser searches.
  - Blocked-tags management page: Added full block-tag picker/list in settings with sorting and filtering controls.
  - Blocked-tags source fetch: Added "Fetch all tags" workflow from paginated popular tags pages until data ends.
  - Blocked-tags search bar: Added quick local search inside blocked-tag management.
  - Blocked-tags toggles: Separate options for applying blocked tags to browser home vs search/open flows.
  - Blocked-tags reset action: Added quick reset for blocked-tag filter state in settings.
  - Share intent support: Sauce Tracker appears as a share target and pastes shared text/link directly into search.
  - Share-lock security fix: Share-open path now respects app lock (no bypass when PIN/biometric is enabled).
  - Browser clipboard import prompts: Copying supported IDs/links in browser can prompt immediate add/update without leaving browser.
  - Browser-library status mapping: Browser cards now reflect local library state (read/rating/imported) when the item already exists.
  - Browser to-library safety prompt: Editing rating/read on non-imported browser items prompts to import first.
  - Browser auto-import on save: Saving rating/read from browser can import missing item automatically and persist status.
  - Incognito restrictions expansion: Disabled sensitive actions in incognito where needed (pin/read/open/copy pathways).
  - Incognito visibility hardening: Browser/library indicators and sensitive metadata are obscured in incognito mode.
  - Incognito toggle protection: Incognito mode toggle can require app-lock authentication.
  - Entry layout control: Added settings for configurable entry columns with preview.
  - Gallery layout mode: Added pure gallery-style layout mode for entries.
  - Gallery mode safety prompt: Enabling gallery mode warns/assists with required thumbnail setting.
  - Thumbnail preload controls: Added startup preload percentage control (0-100%).
  - Startup preload stability fixes: Improved launch flow so preload-heavy startup is less crash-prone.
  - Default collapsed startup: Entries/tags/artists sections load collapsed by default on app start.
  - Collapse behavior corrections: Artists/groups visibility no longer depends incorrectly on entries panel state.
  - Auto-selection fix: Removed unwanted first-item auto-select in common expand/sort/open flows.
  - Series detection engine: Added fuzzy sequence detection to find related entries in multi-part works.
  - Series quick navigation: Selected entry can show previous/next related part buttons when detected.
  - Series privacy alignment: Series navigation controls are hidden/disabled in incognito mode.
  - Pin/unpin safety prompts: Added confirmation dialogs for pin and unpin actions.
  - Pin privacy alignment: Pin/unpin actions are blocked in incognito mode.
  - Settings analytics card: Added a dedicated stats section in settings.
  - Analytics time ranges: Stats can be viewed by `today`, `week`, `month`, `year`, and `all time`.
  - Analytics metrics: Added read totals, pages read, and top-used tags + top creators/groups.
  - Analytics top-lists tuning: Top tags and top creators/groups now show top 5 each.
  - Incognito stats behavior: Stats values/lists are privacy-obfuscated in incognito while range chips (today/week/month/year/all time) remain visible and usable.
  - Selected-entry action placement: Moved `Re-fetch` and `Delete` into the selected-entry code row for faster access.
  - Package/dependency cleanup: Removed Gecko dependency path from current browser stack, reducing APK size.
  - Signed 1.2 release: Built as Sauce-Tracker-1.2-release.apk (versionCode 5, versionName 1.2).



### Sauce Tracker 1.3 - New Features--------------------------------------------------------------------------------------------------------------------------------------

  - Scope note: This section lists changes introduced after 1.2. All 1.2 features (including unified search and accent customization) remain available in 1.3.

  - Desktop Bridge mode: Added a live local-network desktop companion view that mirrors library data from the phone.
  - Desktop unlock challenge: Desktop Bridge now requires answering an on-device code challenge before data access.
  - Multi-round desktop unlock: Desktop Bridge challenge uses 3 consecutive correct rounds before unlock.
  - Progressive lockout policy: Repeated unlock failures trigger escalating temporary lockouts.
  - Desktop unlock hardening: Wrong challenge selection now rotates the active code immediately.
  - Challenge anti-bruteforce flow: On wrong selection, desktop choices refresh and app-side bridge code updates at once.
  - TLS desktop transport: Bridge now serves over HTTPS (`https://phone-ip:port/`) using in-app certificate assets.
  - App-layer crypto channel: Added ECDH + AES-GCM encrypted bridge-state exchange for supported browsers.
  - Compatibility fallback mode: If browser WebCrypto is unavailable, bridge falls back to TLS-only compatibility mode.
  - Stable desktop URL format: Bridge URL now uses a bookmark-friendly base path (`https://phone-ip:port/`) instead of query-token links.
  - Token handling refinement: Desktop auth token is embedded server-side in delivered page script while API endpoints remain protected.
  - Desktop mode visibility: Desktop UI now shows current sync mode explicitly (`Locked`, `Encrypted`, or `Compatibility`).
  - Desktop fallback resilience: On encrypted-handshake failure, desktop auto-falls back to compatibility sync instead of blank state.
  - Desktop entries overview: Added desktop entry cards with thumbnails, metadata, tags, creator chips, and action controls.
  - Desktop pin controls: Pin/unpin works directly from desktop cards with live sync to app data.
  - Desktop read-state controls: Read/Unread state can be toggled from desktop cards.
  - Desktop rating controls: Per-entry star rating + reset available directly in desktop cards.
  - Desktop delete control: Entry deletion is available from desktop cards with confirmation.
  - Desktop open action: One-click open to website from desktop cards.
  - Desktop code quick-copy: Entry code is inline, accent-colored, and clickable to copy to clipboard.
  - Desktop code-first title layout: Entry code now renders before title text so code is always visible on long names.
  - Desktop pin visual redesign: Replaced old pin look with a cleaner Material-style pin indicator that follows accent styling.
  - Desktop card alignment polish: Improved grid/card sizing so rows align more consistently.
  - Desktop image viewer: Thumbnail click opens full-screen preview overlay in desktop mode.
  - Desktop sort/filter parity: Desktop includes entry sorting plus tag and artist/group filtering panels.
  - Desktop rating-sort toggle polish: Rating sort chip simplified to color-state toggle without extra on/off text.
  - Desktop tag panel placement fix: Tags panel positioning now clamps within the main UI surface and avoids visual bleed.
  - Desktop creator panel placement fix: Artists/Groups panel receives the same in-bounds clamping behavior.
  - Desktop columns control: Added/kept 1-6 column density slider for compact/expanded desktop card layouts.
  - Desktop slider theming: Columns slider now uses accent-colored track/thumb styling.
  - Desktop slider fill behavior: Progress fill now renders cleanly at min/max without edge gaps.
  - Desktop accent controls: "Accent color" label and accent-styled control integrated into top desktop toolbar.
  - Desktop accent sync: Accent mode updates are synchronized between desktop bridge and app state.
  - Desktop theme cycling: Desktop bridge supports Auto/Dark/Light theme cycling.
  - Desktop screen blackout control: Added desktop toggle to black out/on phone display while bridge is active.
  - Desktop search hashtag parity: Desktop search now treats `#123456` the same as `123456`.
  - Desktop tab icon: Browser tab/bookmark icon is served for the bridge page.
  - Process-lifecycle bridge safety: Fresh app process launch now always starts with desktop bridge disabled.
  - Manual-enable bridge policy: Desktop bridge requires explicit user enable each new app launch session.
  - Recents-removal behavior: Added task-removal monitoring so bridge is stopped when app task is removed from Android recents.
  - Selected-entry open behavior split: In selected entry, `Open in browser` now opens gallery overview while tapping expanded thumbnail opens page 1 reader.
  - Artist long-press behavior update: Long-pressing artist/group name now opens that page in the in-app browser (clipboard copy removed).
  - Browser quick actions update: Added in-browser pin control using the same pin icon asset as the main app, positioned in the browser card corner.
  - Browser entry removal action: Added in-browser remove option for the current entry with immediate local library update.
  - Browser scroll-state isolation fix: Artist/tag route opens now start at top and no longer inherit scroll from the previous page, while back restores previous route position.
  - Live status refresh improvements: Pin/read/removal status now refreshes immediately when returning from browser and during entry expand/collapse/navigation flows.
  - Incognito safety hardening: Expanded-thumbnail browser open is blocked in incognito mode, and Desktop Bridge cannot be started while incognito is enabled (and is auto-stopped when toggled on).
  - Desktop bridge image open behavior: Tapping the enlarged desktop-bridge thumbnail now opens that gallery directly at page 1.
  - Default layout preset update: Fresh/default state now starts with thumbnails enabled and 2-column pure gallery entry layout.
  - Signed 1.3 release: Built as Sauce-Tracker-1.3-release.apk (versionCode 6, versionName 1.3).



### Sauce Tracker 1.4 - New Features--------------------------------------------------------------------------------------------------------------------------------------

  - Scope note: This section lists changes introduced after 1.3. All 1.3 features remain available in 1.4.

  - Suggested entries platform: Added a full `Suggested entries` section on Home, generated from local read/rating history and metadata.
  - Suggested scoring feed: Suggestions are ranked by weighted local preference signals (tags + artist/group + metadata confidence), not random picks.
  - Suggested candidate expansion: Suggestion refresh now builds multiple fallback search queries and scans multiple pages to avoid sparse result sets.
  - Suggested anti-duplication filter: Imported and hidden codes are removed before rendering suggestions.
  - Suggested overflow queue: Suggestions keep an overflow pool so skipped items can be replaced immediately.
  - Suggested score preview: Each card now shows a live relevance score.
  - Suggested tag preview: Suggested cards show top-ranked matching tags for quick context.
  - Suggested quick actions: Added direct `Open`, `Skip`, and `Import` actions on each suggested card.
  - Suggested skip semantics: `Skip` is non-destructive (not hidden/blacklisted) and immediately replaces the card from suggestion overflow.
  - Suggested swipe actions: Suggested cards support deliberate swipe gestures for quick pin and read/unread updates.
  - Suggested long-hold actions: Suggested cards support hold-drag actions (`Cancel` / `Don't show again`) with overlay feedback.
  - Suggested gesture import-assist: Suggested swipe/hold actions can auto-import missing local entries before applying state updates.
  - Suggested incognito safety: Suggested action gestures are blocked in incognito mode.
  - Suggested incognito masking: Suggested list content is hidden/masked while incognito mode is enabled.
  - Suggested empty-state guidance: Added clearer empty/info states that explain how to bootstrap better suggestions.

  - Suggestion weight controls: Added slider controls per category in Suggested settings.
  - Suggested weights quick access: `Weights` opens the category-weight panel directly from Suggested header even when the suggestions list is collapsed.
  - Suggestion categories: Weights now include `General Tags`, `Parody`, `Character`, `Category`, `Language / Translation`, `Artist / Group`, and `Other`.
  - Weight scaling range: Category sliders run 0%-200% (with 100% baseline).
  - Live weight re-ranking: Changing weights while suggestions are visible triggers immediate refresh/re-ranking (outside incognito).
  - Weight reset action: Added `Set To Default` to restore default weighting profile.
  - Rating-0 neutrality in suggestions: Rating `0` is treated as neutral (not negative) in suggestion scoring logic.

  - Search parity for suggestions: Suggested filtering now follows `Search everything` semantics (title/author/group/tags/language/etc.).
  - Tag-filter parity for suggestions: Active local tag filters are now enforced in suggestion candidate filtering.
  - Blocked-tags parity for suggestions: Suggestion query generation now applies blocked-tag exclusions, aligned with browser/home exclusion behavior.
  - Search/filter change handling: Search or tag-filter changes invalidate stale suggestions and prompt refresh with clear status messaging.

  - Hidden suggestions shared model: Suggested and browser hide actions now write to the same hidden-suggestion data store.
  - Hidden suggestions with timestamps: Hidden entries now keep hide timestamps (not only code list).
  - Hidden list ordering fix: Hidden suggestions are sorted by hidden time (newest first), then code.
  - Hidden suggestions manager UX: Settings -> Data now has an expandable, scrollable hidden list manager.
  - Hidden metadata visibility: Hidden manager rows show code + hidden timestamp for easier recovery decisions.
  - Hidden list thumbnails: Hidden entries show mini thumbnails for easier visual recovery/unhide.
  - Hidden-thumbnail prefetch: Missing hidden thumbnails are fetched in background for better manager usability.
  - Hidden-action confirmations: Added confirmation dialogs for both unhide and clear-hidden operations.
  - Incognito data masking: Hidden-suggestion management data is masked while incognito is active.

  - Duplicate detection engine upgrade: Added robust duplicate scoring against local library seeds.
  - Thumbnail visual duplicate matching: Duplicate checks now use perceptual thumbnail hashing (dHash + Hamming similarity), not just URL/id equality.
  - Duplicate metadata scoring: Duplicate confidence combines title similarity, tag overlap, artist/group overlap, pages, upload date, and media signals.
  - Duplicate false-positive guard: Creator-only weak overlaps are filtered to reduce noisy duplicate flags.
  - Artist-mismatch strictness: Artist mismatch paths now require very strong thumbnail + metadata agreement.
  - Suggested duplicate indicator: Suggested cards now show `Duplicate? #code` with a reason summary.
  - Browser duplicate indicator: Browser list cards now also show duplicate hints and reasons.
  - Duplicate deep-link action: `Duplicate?` is now a text button that opens the matched code in browser overview.
  - Duplicate loading state: Browser cards now show `Checking duplicate...` while duplicate scoring is computed.
  - Duplicate reason layout polish: Duplicate reason text is compact and aligned with the action row to reduce card height growth.

  - Browser long-hold hide action: Browser card hold-drag rating overlay now includes a `Hide` action after the 5-star slot.
  - Browser hidden-code filtering: Browser list loading now excludes hidden suggestion codes automatically.
  - Browser/suggested hide sync: Hide/unhide state is shared, so hiding in browser immediately affects suggested cards and vice versa.
  - Browser hold/swipe haptics: Browser card thresholds now emit haptic feedback on hold/swipe confirmation.
  - Browser deliberate swipe gating: Browser swipe actions now enforce distance, direction-dominance, duration, and speed requirements.

  - Local hold popup width standardization: Local-entry hold rating popup now uses a consistent near-full-screen width regardless of 1x/2x/3x grid mode.
  - Local hold selection mask: Held local entries remain visibly greyed so the relation between popup and card stays clear.
  - Local hold z-order fix: Hold popup now renders above entry cards (overlay layering corrected).
  - Local hold cursor mapping fix: Hold drag-to-star mapping now uses absolute popup/screen coordinates for consistent star targeting at all row densities.
  - Entry swipe safety hardening: Local entry swipe pin/read now requires deliberate horizontal motion within strict speed/angle/time constraints.
  - Entry swipe threshold haptics: Added haptic feedback when swipe threshold is crossed.

  - Suggested list height parity: Suggested list vertical window now scales with the same max-height model used by artist/group lists.
  - Suggested list interaction polish: Suggested list uses the same lazy-list interaction handling style as tags/artists sections.
  - Suggested hold haptics: Hold-start and hold-drag target changes now trigger haptic feedback during suggested card long-hold actions.
  - Search/filter persistence fix: `Search everything` and active tag filters now persist across browser open/close flows.

  - Analytics activity heatmap: Added `Activity` heatmap to Settings -> Analytics (calendar-style week/day grid).
  - Heatmap range chips support: Heatmap responds to existing analytics range chips (`Today`, `Week`, `Month`, `Year`, `All Time`).
  - Heatmap day details dialog: Tapping a heatmap day shows date + pages read + entries read.
  - Daily activity storage: Added persistent `daily_read_activity` table (`activity_date`, `pages_read`, `entries_read`).
  - Read/unread reversible aggregates: Daily activity increments on unread->read and decrements on read->unread (clamped at zero).
  - Slideshow session telemetry: Reader now records session start/end, start page, furthest page, pages viewed, and elapsed seconds.
  - Reading sessions table: Added persistent `reading_sessions` storage for reader analytics.
  - Reading speed metrics: Analytics now computes avg pages/min, total reading time, and total pages viewed per selected range.
  - Reading calibration guardrail: Speed output is suppressed until enough data is collected, with explicit guidance message.
  - ETA integration: Selected entry can show estimated time-to-finish based on calibrated reading speed.

  - Backup export expansion: Backups now include hidden suggestions, hidden timestamps, suggestion weights, entry pin-priority setting, daily activity, and reading sessions.
  - Backup import expansion: Import restores the same new datasets (hidden suggestions/timestamps, weights, pin-priority, activity, sessions).
  - Procedural backup status expansion: Auto-backup status now reports counts for hidden suggestions, weights, daily activity rows, and reading sessions.

  - App entries pin-priority toggle: Added `Pin` priority toggle in app entry sort chips.
  - Desktop entries pin-priority toggle: Added matching pin-priority toggle in desktop bridge action row (persisted as local setting).
  - Desktop entries read filter controls: Added desktop `All / Read / Unread` entry filter buttons.
  - Desktop bridge parity updates: Desktop filtering/sorting behavior now stays aligned with newer app-side entry controls.

  - Normal-mode entries recovery: Fixed a regression where normal layout entry rows could render empty after gesture/layout iterations.

  - Signed 1.4 release: Built as Sauce-Tracker-1.4-release.apk (versionCode 7, versionName 1.4).



### Sauce Tracker 1.4.5 - UI Refine Update--------------------------------------------------------------------------------------------------------------------------------------

  - Scope note: This release focuses on browser/list polish, duplicate-check performance, transition smoothness, and backup-backed thumbnail reuse.

  - Browser swipe refinement: Pin/read browser gestures now match detail-view import prompting behavior for missing local entries.
  - Swipe confirmation polish: Swipe haptics now trigger when release will actually commit, not only after lift or at a loose threshold.
  - Shared swipe behavior: Browser and local entry swipe-commit logic now uses the same confirmation model and visual timing.
  - Swipe visual polish: Armed-state swoosh, pin/read color consistency, and swipe settle behavior were refined for a smoother feel.
  - Browser page-turn polish: Entry/detail navigation now uses a smoother book-style transition with matched forward/back motion.
  - Browser launch polish: Direct open from local entry now goes straight to the target gallery instead of flashing the browser home page first.
  - Browser transition cleanup: Search/header disappearance, list bumps, temporary loading text, and premature layout shifts were removed from detail opens.
  - Slideshow transition polish: Gallery slideshow now fades from its loading surface into the first page instead of popping in.
  - Local creator jump animation: Tapping artist/group from a selected entry now expands the destination section first and transitions there more smoothly.

  - Duplicate-check performance upgrade: Browser duplicate checking now uses indexed local seed matching instead of repeatedly scanning the full local set.
  - Backup hash preload: Archived local duplicate hashes are bulk-loaded into memory for browser matching, reducing repeated backup reads.
  - Duplicate-check parallelism: Duplicate checks now scale better across visible rows while keeping work bounded.
  - Thumbnail-first browsing: Browser covers get a head start so scrolling stays smooth while duplicate checks continue in the background.
  - Duplicate hint styling refresh: Duplicate results now render as lightweight inline text with the full-card shimmer preserved behind the non-thumbnail content.
  - Red design-state standardization: Custom destructive/negative design states now align to the same fixed action red used by swipe gestures.

  - Local backup thumbnail reuse: Local and browser thumbnail loaders now prefer archived local thumbnails first and only fall back to network if needed.
  - Launch preload fix: `Load data on launch` now warms the same local-first thumbnail path the entry UI actually uses, preventing `No preview` flashes.
  - Launch preload tuning: Top visible entry thumbnails are prioritized to stay hot in cache for immediate expand behavior.
  - Backup archive structure: Procedural thumbnail/hash backups now live inside a dedicated `SauceTracker Backup` subfolder instead of cluttering the selected folder root.
  - Backup privacy polish: Mirrored backup thumbnails are stored as app-specific archive blobs instead of obvious raw `.jpg` files.
  - Backup speed upgrade: Thumbnail/hash archive sync now indexes folders once and processes missing items in parallel for much faster backup runs.
  - Backup status feedback: Settings now shows thumbnail-archive progress and mirrored-local counts so backup reuse is visible.

  - Browser duplicate shimmer polish: Duplicate-found shimmer now turns the same fixed action red used by negative gesture states.
  - Suggested/local duplicate visuals: Duplicate and hide-state visuals were aligned more closely with browser behavior and overall UI language.

  - Tag graph explorer: Added an interactive graph view in Settings -> Data with `Tag heatmap`, `Raw frequency`, and `Rated frequency` tabs.
  - Heatmap interaction layer: Graphs now support pinch-zoom, pan, reset, and selected-node focus.
  - Tag similarity mapping: Heatmap clustering now uses local co-occurrence similarity so related tags can influence later recommendation logic.
  - Strongest-neighbor focus: Selecting a tag can isolate its strongest local neighbors for easier validation of the heatmap structure.
  - Entry heatmap mode: Heatmap can render local entries as thumbnail nodes positioned by dominant tag-family placement.
  - Entry-family overlay: Entry heatmap includes optional family-outline logic to visualize the major tag-group islands behind thumbnail placement.
  - Graph privacy mode: Incognito now hides sensitive graph details, disables graph interactions that expose data, and obscures thumbnail content.

  - Suggestion theme matching: Suggested entries now score not only exact tags but also locally learned theme-neighbor tags from the heatmap similarity model.
  - Suggestion explanation text: Suggested cards now show a `Why suggested?` reason summary using the same side-slot as duplicate explanations.
  - Theme-strength control: Suggested-entry weights now include a dedicated `Theme strength` slider alongside the category weights.
  - Suggestion session reroll behavior: Refresh/skip handling was refined so current-session exclusions stay temporary instead of acting like permanent hides.

  - Signed 1.4.5 release: Built as Sauce-Tracker-1.4.5-release.apk (versionCode 8, versionName 1.4.5).



### Sauce Tracker 1.5 - Heatmap + Local Download Update--------------------------------------------------------------------------------------------------------------------------------------

  - Heatmap Overview on home: Added a large interactive `Heatmap Overview` card directly on the main screen with `Tags / Entries` switching.
  - Home heatmap interaction parity: Home heatmap now supports the same tap-driven tag/entry inspection flow as the settings graph.
  - Graph bottom-sheet navigation: Back gestures inside graph/tag/entry drill-down now step through the graph flow correctly instead of exiting too early.
  - Graph detail redesign: Tag taps open a filtered local-entry list; entry taps open the full selected-entry style detail sheet.
  - Graph prevalence bars: Tag/entry graph sheets now show `Website / Library / Relative` prevalence summaries in a full-width header strip.
  - Graph sheet polish: Home/settings graph sheets gained rounded drag-handle expansion behavior and cleaner header placement.
  - Graph performance caching: Thumbnail/session caches and saved entry-layout cache reuse reduce repeated graph recomputation work.
  - Manual entry-heatmap rebuild: Added `Recalculate Entry Heatmap` in Settings with explicit confirmation/progress flow.
  - Saved entry heatmap cache: Entry heatmap layout is now stored and reused instead of being recomputed automatically every app launch.
  - Entry heatmap overhaul: Reworked the saved `Entries` heatmap toward clearer family/island structure with circle-first family placement.
  - Family-outline refinement: Entry heatmap family circles now better follow final packed entry footprints and avoid obvious offset drift.
  - Entry heatmap privacy hardening: Incognito mode now hides graph details and disables graph interactions that would reveal sensitive info.

  - Local gallery downloads: Selected local entries now support direct per-entry gallery download to a configurable downloads folder.
  - Download prompt flow: Downloading shows intro/confirm dialogs with remembered default-folder behavior.
  - Local slideshow source path: Downloaded galleries now open slideshow from local files instead of the web path.
  - Local gallery info page: Download bundles include a generated first info image with title / artist / code / cover for use outside the app.
  - Downloaded-folder integration: `Local` actions now route directly to the correct downloaded entry folder flow.
  - Local delete split options: Entry delete prompts can now remove app entry only, local files only, or both.
  - Re-download support: Long-pressing `Local` can replace an existing local download with a fresh copy.
  - Downloaded filter: Entries view now includes a `Show Downloaded` filter mode.
  - Batch gallery downloads: Download and re-download prompts now support multi-select batch download mode with progress feedback.
  - Download intro flow refinement: The first local-download popup now stays focused on folder choice (`Change Folder / Cancel / Continue`), while batch download remains on the second confirmation step.
  - Gallery media hiding: Download root writes `.nomedia` so downloaded pages stay out of normal gallery apps.

  - Reader mode chooser: Gallery slideshow now supports both horizontal and vertical reading layouts.
  - Hold-hover release picker: Reader mode selection now uses a hold-and-drag chooser matching the star-selector interaction style.
  - Reader mode persistence: Chosen slideshow layout mode stays remembered instead of resetting each time.
  - Vertical reader polish: Vertical mode shows clean page-number-only overlays without filename clutter.
  - Reader handoff polish: Switching between horizontal and vertical modes now preserves the current page instead of resetting to page 1.
  - Reader immersion mode: Middle-tap can fade slideshow chrome in and out for an image-only reading view.
  - Reader direction setting: Horizontal slideshow now supports `Western` and `Manga` reading direction from settings.
  - Reader transition polish: Mode-switch and immersion animations were smoothed, with vertical launch/reveal behavior aligned more closely to horizontal mode.
  - Reader stability fixes: Vertical mode regained proper inertial scrolling and stable center-based page tracking after earlier gesture regressions.

  - Local selected-entry polish: Added download/local actions into the selected-entry code/action row and refined related confirmation flows.
  - Selected-entry accent fix: Local selected-entry title highlighting now follows the app accent color instead of device accent leakage.
  - Selected-entry read timestamp: Local selected-entry detail now shows `Read at` directly under the fetched date when available.
  - Local entry read-date sort: Local entries now include a `Read` ascending/descending sort alongside title/pages/uploaded/fetched.
  - Browser/incognito/graph polish: Multiple visual refinements were made across graph sheets, reader transitions, and privacy-safe graph rendering.
  - Timezone display fix: `Added`, `Fetched`, and `Read` timestamps now render in local timezone instead of looking one hour off.
  - Browser back-stack sort state: `Recent / Today / Week / All Time` selection is now preserved when opening a gallery and backing out.
  - CSV export: Settings now include a CSV export path for library data and related stats summaries.
  - Subscription system: Tags and artists/groups now support subscribe bells, a `Subscriptions` home section, notification settings, manual refresh, and import/export through backups.
  - Subscription polish: Added a subscriptions list popup, incognito handling, bell permission prompt on first subscribe, and more consistent subscription row behavior.
  - Suggestion weighting expansion: Suggestions now include a `Length / Pages` weight and stronger language-specific matching.
  - Suggestion matching cleanup: `translated` / `translation` no longer influence suggestion preference weighting or candidate matching.

  - Signed 1.5 release: Built as Sauce-Tracker-1.5-release.apk (versionCode 9, versionName 1.5).



### Sauce Tracker 1.6 - Dashboard, Reading History + Performance Update--------------------------------------------------------------------------------------------------------------------------------------

  - Modern dashboard: Added an optional full-page Home dashboard while preserving the original Home layout as a Legacy UI setting.
  - Dashboard navigation: Entries, Tags, Artists / Groups, Suggested entries, Subscriptions, Heatmap, and Reading History now open as dedicated full-page surfaces with remembered in-app state.
  - Dashboard polish: Dashboard surface changes now reveal smoothly, dashboard cards use shape-matched press feedback, and widget pager pages have intentional spacing.
  - Atomic theme updates: Light/Dark/Auto and accent changes update app content and Android system bars together without returning the user to the dashboard.
  - Transition polish: Dedicated dashboard pages now keep the Search Everything panel stable while page content fades in without the earlier flicker.
  - Modern Tags and Artists / Groups: Added full-width visual list pages with sorting, subscriptions, expanded creator entries, and direct entry detail expansion.
  - Creator routing: Artist/group links inside entry details now navigate to, expand, and scroll to the matching modern creator card.
  - Dashboard discovery: Suggested and Random entries share a swipeable dashboard widget, with swipeable larger previews for random covers.
  - Subscription dashboard: Added a swipeable Subscriptions / Heatmap / Reading History widget plus full pages for updates, heatmap, and reading history.
  - Subscription inbox: Subscription updates are now enabled by default with read/pin/dismiss handling and dashboard-style presentation.
  - Reading history: Added persistent per-session read history, re-read recording, editable/removable history cards, average ratings, and daily activity entry drill-downs.
  - Rating presentation: Entry cards, selected detail, and browser views use the current averaged rating rather than only the first read rating.
  - Filter chips and shortcuts: Active tag filters now render as removable inline pills, clear actions are available directly from Search Everything, and dashboard long-press shortcuts are enabled by default.
  - Browser improvements: Creator/tag browsing supports reliable pagination, local-library status, duplicate-check mode controls, full-screen duplicate menu dimming, and rounded pinned-card glow treatment.
  - Browser incognito polish: Browser incognito mode now switches with a themed transition cover so privacy/theme rebuilds do not expose partial reload flicker.
  - Browser privacy hardening: Browser result titles/codes and slideshow top-bar titles are now obfuscated in incognito mode.
  - Entry detail polish: Selected-entry detail loading now keeps the tapped card responsive first and fades heavier metadata in afterward by default.
  - Browser performance: Browser duplicate work pauses during scrolling and batches local-library lookups; local library thumbnail archives are indexed for faster reuse.
  - Adaptive image loading: Entry and browser views use lower-cost thumbnails while scrolling and restore the normal image once idle; startup preload warms the same archive-backed thumbnail path.
  - Performance controls: Added default-on adaptive scrolling thumbnails and an optional diagnostics overlay for frame rate, thumbnail throughput, duplicate checks, and cache hits.
  - Thumbnail loading: Added batched loading for local and browser entries giving 31x increase in load time.
  - Legacy preservation: Legacy Home visual structure remains available and separate from dashboard-specific presentation work.
  - Signed 1.6 release: Built as Sauce-Tracker-1.6-release.apk (versionCode 10, versionName 1.6).
