# Debug device QA — 2026-10-03

Device: Nothing Phone 3 (A024). Package: `com.roinur.saucetracker.rewrite`,
version code 14, debug. Stable package was not installed, reset or restored.
No screenshot was taken outside GitHub media mode.

## Confirmed

- Latest debug installed in place without clearing app data.
- On-device Library Health exported current data, parsed it and restored it twice
  into a disposable Android SQLite database. The second restore was idempotent,
  with zero skipped rows and lossless source-platform/session/subscription/settings
  comparison. Dataset: 1,584 legacy entries plus 10 MangaDex source entries,
  80 MangaDex sessions, 9 reader positions and 14 chapter-progress rows.
  No MangaDex subscriptions were present, so their real-data round trip is not
  covered by this result.
- SQLite quick check passed; zero missing legacy source identities, orphan profile
  states or orphan tag relations; ratings in range.
- QR Share opened, generated a MangaDex QR and opened Android's share chooser.
  Sharing was canceled; no content was sent externally. Camera and saved-image
  scanning are not covered by this smoke test.
- GitHub-mode startup initially crashed on a foreign-key constraint while replacing
  default source rows. Reproduced and fixed with dependency-ordered transactional
  destination cleanup/copy; production tables are only read.
- Final local verification after that fix passed: 112 JVM tests, SQLite/Bridge
  regression scripts, profile APK and lint vital.
- The fixed debug APK was installed again in place. GitHub media mode then opened
  successfully; the captured dashboard's private thumbnails/titles were masked.
  All captures used the guarded GitHub capture helper and stayed under ignored
  `build/github-media-review` (not published).
- Created a combined QA profile and copied one MangaDex entry through the real
  Settings/Profiles UI in the media database: one copied, zero failed. A read-only
  SQLite/WAL audit after stopping debug compared the temporary device replicas:
  rating/read/pin matched; one resume position, one chapter-progress row and 14
  reading sessions matched the original source profile. Shared legacy/source
  entries and tags/creators matched production; the QA profile existed only in
  media. Temporary database replicas were removed after the audit.
- Version Museum opened and loaded a screenshot and its expanded preview.
  Historical provenance/dates are still unverified; this is a UI/load smoke test.
- Debug was returned to its normal launcher after isolated testing.

## Follow-up diagnosis and safeguards

- Read-only device audit found the 71 unlinked sessions comprise 55 NHentai legacy
  sessions and 16 MangaDex sessions. Their profiles exist, remote identities are
  nonblank, and neither legacy entries nor alternate language identities remain.
  Reader code can persist browser-only sessions; unused shared metadata cleanup
  intentionally leaves reading sessions. Lack of current library metadata alone
  is therefore not evidence of a broken migration. The original cause of each
  individual historical removal/browser session cannot be reconstructed.
- The current rolling file has since become a complete V2 export (20, source
  schema 5): 1,594 shared entries and 767 sessions, including 80 MangaDex sessions.
  All 13 source-platform tables match the stopped debug database's private replica,
  field for field. Current and Previous 1 SHA-256 values were unchanged across the
  read-only audit; no audit code wrote or rotated either backup.
- Previous 1 is the earlier incomplete file: 1,931 legacy entries, mixed legacy/new
  session field shapes and 80 MangaDex sessions, but no source-platform payload.
  This cannot faithfully restore the missing MangaDex metadata/profiles/progress.
  It was retained, not repaired by inventing data or merging current state into it.
- Added preflight at both parser and database-import boundaries to reject missing
  multi-source owners/payloads before writes. Valid browser-only history is allowed
  without requiring a current library entry.
- Verified Restore now compares legacy session/subscription rows with precisely
  defined new-field defaults; supplied fields and row multiplicity remain strict.
- Library Health distinguishes invalid profile/source identities (action required)
  from preserved sessions without current metadata (attention). No history is
  deleted or fabricated. Read-only backup lookup no longer creates directories or
  rewrites `.nomedia`.
- Post-edit `sauce.bat verify -NoInstall` passed: 116 JVM tests with zero failures,
  all local SQLite/Bridge regressions, profile APK and lint vital. The new backup
  tests cover old/defaulted versus explicit ownership, changed page counts and row
  multiplicity, incomplete files and browser-only sessions.
- ADB disconnected after the read-only audit, before the updated APK could be
  installed. Updated on-device Library Health/Current-file restore is pending;
  compilation and synthetic checks do not replace that check.
- `sauce.bat fast -NoInstall` subsequently built the updated debug APK successfully.
  It is ready for an in-place debug update; no stable APK was installed.

## Provider parity and Sauce Finder index priority (local implementation pass)

- Shared library search now compiles structured fields and preset OR/exclusion
  filters before database pagination. Source tags scope providers; chapter counts
  do not match page-count filters. Creator/author and language routes preserve
  categories and translate human language names to provider codes.
- Removed the full-library 1,000-entry truncation, not an API rate-limit safeguard.
  Database hydration uses bounded pages and batched tag/creator queries; remote
  page sizes and bounded request concurrency remain intact. QR selection pages
  through larger libraries without changing its ten-entry package limit.
- Training and recommendation taste data come from the active profile/source
  scope. MangaDex uses the established scorer, category weights, explicit feedback,
  block tags, presets and hidden/skipped state. Mixed results are provider-balanced.
  Bridge reuses phone scores or that same scorer instead of tag-only fallback.
- Launch preload reuses the normal source-aware library projection. Local heatmaps
  do not require a remote NHentai tag fetch. Catalog merging keeps block-state row
  identities; Re-fetch All uses each adapter and defers a rate-limited provider.
- Sauce Finder queues downloaded entries first, read entries second and the rest
  last, preserving order within each tier. MangaDex covers are supported; this is
  not a full MangaDex chapter-page image index and does not download whole chapters.
- Final post-edit `sauce.bat verify -NoInstall` passed: 135 JVM tests, zero failures
  or errors, all isolated SQLite/migration/backup/Bridge regressions, profile APK
  and lint vital. New regressions cover structured source/preset filters, language
  translation, large local libraries/choosers, shared creator scoring, unit labels,
  provider balancing, hidden/skipped/imported visibility and index queue priority.
- `sauce.bat fast -NoInstall` then built the matching debug APK successfully;
  package `com.roinur.saucetracker.rewrite`, version code 14, API 26 minimum.
- No phone installation, restore, reset or production-data writes were performed
  during this pass. Live-provider/device parity still requires separate checking.

## Extra dark, deferred museum and default profiles (follow-up pass)

- Installed successive debug updates in place on the same A024 phone; no stable
  install, uninstall, database reset or production restore was performed.
- Extra dark reuses Book Tracker's off-by-default, dark-only surface palette.
  GitHub-mode UI checks compared on/off captures: Dark backgrounds changed to
  `#0B0A12`, while Light backgrounds stayed `#F7F5FF` with the toggle on or off.
  Accent/error colors are preserved by the shared palette helper's JVM tests.
  All screenshots used the guarded GitHub capture helper; test preferences were
  changed only in the separate media session.
- Exercised MangaDex structured chapter search, selected-entry chapter list and
  Popular Now browser in the media copy. The initial 12-result suggestions test
  was in the NHentai Main profile, not MangaDex. The explicitly selected MangaDex
  profile had no training/positive-rating seed and correctly showed an empty
  recommendation state; populated live MangaDex recommendations are not covered
  by this smoke check.
- Commented out Version Museum's navigation, activity and generated screenshot
  assets for 2.0. The implementation/manifest source remains. Experimental Gallery
  opened on device without a Museum button; the debug APK had no museum asset
  entries. Historical museum verification is deferred, not a 2.0 release gate.
- Production SQLite migration SQL creates exactly two source-locked defaults for
  fresh installs / 1.9 upgrades: NHentai and MangaDex, initially opening NHentai.
  Empty, one-entry and 2,500-entry fixtures preserve all old state in NHentai;
  rollback/retry and repeated setup pass. Existing 2.0 profiles and a deliberately
  deleted MangaDex default remain unchanged on subsequent startup.
- Four JVM routing tests require V1 backups to target NHentai even with MangaDex
  active or a different profile explicitly supplied; V2 selected/full restore
  retains its previous scope. Import UI selects NHentai before applying legacy
  profile preferences, preserving the previous profile's taste state.
- These default-profile / V1 routing checks are isolated tests, not an on-phone
  1.9 upgrade or production restore. Debug/release package separation was confirmed
  in the build configuration; no automatic import of debug profiles is performed.
- Final post-edit `sauce.bat verify -NoInstall` passed: 144 JVM tests, zero failures
  or errors, all isolated SQLite/migration/backup/Bridge regressions, profile APK
  and lint vital. The profile APK was not installed over debug or stable.
- The matching `sauce.bat fast -Device 000241568001131` then passed and installed
  debug in place. Package/version were confirmed as
  `com.roinur.saucetracker.rewrite` / `2.0-rewrite` (code 14); normal MainActivity
  resumed and the existing 1,472-entry active library loaded. Existing development
  profiles were not reset; the two defaults apply to fresh installs / 1.9 upgrades.

## Remaining release gates

- The earlier procedural backup restored idempotently but failed lossless
  comparison for sessions/subscriptions/seen codes/events. It reported 80 MangaDex
  sessions but zero MangaDex source entries. This is not proof that current-data
  exports are broken: the current-data round trip above passed. Existing backup
  files were not overwritten, rotated or deleted by QA. Its incomplete content is
  now established; the new complete Current still needs the updated on-device
  Verified Restore recheck.
- Full provider/reader/privacy/Bridge/browser-installation and realistic interrupted
  upgrade checks remain separate release gates.
