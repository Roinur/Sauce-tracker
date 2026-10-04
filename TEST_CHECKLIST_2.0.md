# Sauce Tracker 2.0 release gate

This is an evidence checklist, not a claim that every device/browser combination has
been tested. On 2026-10-04 the owner explicitly requested publication of 2.0 as a
normal release with the remaining manual coverage disclosed. Unchecked items remain
unchecked; that decision does not turn missing tests into passes. Use isolated
databases for destructive restore/migration cases and never reset the user's app.

## Automated checks

- [x] `sauce.bat verify -NoInstall` passed after this pass's final code edits on 2026-10-03 (116 JVM tests, isolated SQLite/Bridge regressions, profile APK and lint vital); full compilation uses a 3 GB heap and one worker to avoid concurrent compiler memory exhaustion.
- [x] Isolated migration SQL test covers entry state, tags, creators, sessions, subscriptions, events and source-ID collisions.
- [x] Production migration SQL passed empty, one-entry and 2,500-entry 1.9 fixtures, including interrupted migration, rollback, retry, preserved legacy values and repeated schema setup (2026-10-03). The JSON binding step is separately covered by JVM serializer tests.
- [x] Isolated Bridge SQL and reader-state tests pass.
- [x] Real Bridge client tested with deferred synthetic responses: canceled/overlapping reader launches, original-profile progress, stale list/image responses, background and LAN disconnection clearing, bounded image reuse and lazy vertical pages.
- [x] Source-backup column contract covers all persisted source-platform fields; isolated mixed-source/language/profile rows round trip twice with rollback on interruption. This tests production columns/schema, not Android's full backup pipeline.
- [x] Production profile-progress SQL tested for resume preservation, progress merge and repeated copying; profile cleanup and selected-restore deletion scopes preserve other profiles in isolated SQLite fixtures.
- [x] QR codec regression suite: mixed and ten-entry round trips, duplicate generation, compressed/expanded limits, unsupported formats/sources and malformed preview metadata. Camera, image scanning and Android sharing remain separate device checks.
- [x] GitHub media-copy regression reproduces the old parent REPLACE/RESTRICT crash and checks dependency ordering, interrupted-copy rollback, repeated copies and unchanged production rows in isolated SQLite.
- [x] Backup-history regressions cover strict legacy default comparison, incomplete multi-source imports, valid browser-only history, invalid owners and read-only existing-backup lookup.
- [x] Repeated `sauce.bat verify -NoInstall` after the release Heatmap Overview ANR fix (2026-10-04): 162 JVM tests, zero failures/errors; isolated SQL/Bridge regressions, profile APK and vital lint passed.
- [ ] Run `sauce.bat performance` and compare cold/warm startup, browser, reader and large-library scrolling against 1.9.

## Data safety — release blockers

- [x] On-device current-data Verified Restore passed twice in isolated Android SQLite on Nothing Phone 3: 1,584 legacy entries, 10 MangaDex entries, 80 MangaDex sessions, 9 reader positions and 14 chapter-progress rows; zero skipped rows and lossless source/session/subscription/settings comparison. No MangaDex subscriptions were present. See `docs/QA_2.0_2026-10-03.md` for existing-backup/history warnings.
- [x] On-device GitHub-mode copy of one MangaDex entry preserved personal state, resume, chapter progress and 14 sessions; read-only audit confirmed shared data matched production and the QA profile did not exist in production.
- [x] Read-only audit of actual Current backup matched all 13 source-platform tables against the debug device replica; Current and Previous 1 hashes remained unchanged during the audit. Previous 1 is confirmed incomplete (mixed session formats, MangaDex sessions but no source-platform payload); the updated import rejects this instead of claiming a complete restore.
- [x] Updated debug Library Health/Verified Restore checked on 2026-10-04: Healthy, retained history outside Library, 1,931/1,931 restored, zero skipped and second pass idempotent; MangaDex includes 11 entries, 3 sessions, 11 resumes and 16 chapter-progress rows. See `docs/QA_2.0_2026-10-04.md`.
- [ ] Migrate realistic empty, small and large 1.9 databases in isolation; compare every preserved entry, tag, creator, rating, read/pin state, session, subscription, download reference and selected document folder.
- [ ] Interrupt migration and confirm rollback, a valid retained pre-migration snapshot and a successful retry.
- [ ] Export a mixed NHentai/MangaDex library with multiple profiles, chapter progress, sessions and subscriptions; run Verified Restore twice in isolation and compare the restored data to the export.
- [x] Real official 1.9 release upgraded in place on 2026-10-04: exactly locked NHentai/MangaDex defaults, active NHentai, empty MangaDex; all 1,927 original entries and preserved state/history/subscriptions matched fresh before/after exports. Fresh-install behavior additionally covered by production SQL tests.
- [ ] Test V1 import while MangaDex is active: entries, history, subscriptions and preferences must go to NHentai, without changing MangaDex state. Test V2 restore of all profiles versus one selected profile. Confirm a failed restore leaves the original database and manual exports untouched.
- [ ] Test profile copy, merge, move and delete with shared entries on disposable data; check that other profiles and shared media survive.

## Phone and Desktop Bridge — release blockers

- [ ] Test both providers' browse, Search Everything, detail, import, refresh, tags and creators, including partial provider failure and mixed-profile filtering.
- [ ] Read MangaDex chapters forward/backward and resume mid-chapter; confirm pages, chapter completion and reading time appear once in History and Trends. Recheck NHentai reader/rating regressions.
- [ ] Test QR camera and saved-image import, mixed ten-entry packages, oversized/corrupt payloads, duplicate conflict handling and privacy preview.
- [ ] Test Bridge TLS/challenge/rate limit, both browser security modes, profile isolation, browser/reader/history/suggestions, incognito shutdown, disconnection clearing, Chrome/Edge ordinary tabs and site-app windows.
- [ ] Test app lock/incognito across dashboard, popups, subscriptions, QR preview, share sheet and Bridge.
<!-- Deferred beyond 2.0: review Version Museum historical dates and screenshots before re-enabling it. -->

## Release

- [ ] Resolve all blocker findings above, then write final 2.0 release notes and review the README/capability table.
- [ ] Run `sauce.bat release` only after the gates pass; verify signing identity, version code/name and APK SHA-256.
