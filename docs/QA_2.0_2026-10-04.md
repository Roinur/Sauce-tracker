# Sauce Tracker 2.0 — 2026-10-04

## Final-publication pass: release Heatmap Overview ANR

- Publication was paused after the owner reproduced a release-only NHentai Heatmap Overview freeze. Android reported input-dispatch ANRs, not a new Java exception.
- Three actual device ANR main-thread stacks from Android DropBox all pointed to `prepareTagGraphData -> loadPopularTags -> normalizeTagName`. The nested local/catalog scan repeatedly compiled and executed regex normalization on the UI thread.
- Replaced the quadratic scan with a name/category hash index: exactly one normalization per catalog row and local row. Reused the whitespace regex without changing normalization semantics. Catalog preparation now belongs to the existing cancellable IO graph-load job; only the result is published to Compose state on Main.
- Added category/normalization, empty-catalog and 4,460-by-4,000-row regression coverage. The large fixture checks exactly 8,460 normalizations rather than relying on a fragile wall-clock assertion.
- Browser's local screenshots row is now removed; Library retains its existing compact link. Full release notes were assembled from the original ideas, implementation changelog and subsequent decisions; Museum is explicitly deferred, not advertised as shipped.
- Before the final update, a new actual stable V2 export was pulled and hashed: `build/device-qa/final-release-20261004/before-final-v2.txt`, SHA-256 `441510b1f8ca5973139ae16c38534c0fffb81a1da1cc85a23b8c4209c20e603e`. It contains 1,930 legacy entries, 1,931 source entries, two profiles, 858 sessions and populated MangaDex resume/progress. Android current-data Verified Restore passed twice in isolation; it did not replace the production database.

Final build/install and post-fix device results:

- A newer pre-install manual export was then created because the phone had been used during the build: `before-heatmap-release-v2.txt`, SHA-256 `f5d960a3df0133781b1c0029da685162a91899212cc1a357445e48e18561aa4e`. It has 1,933 legacy entries, 1,935 source entries, 859 sessions, two MangaDex entries/resumes/progress rows and two profiles. Compared with the earlier export, three legacy entries were added, none removed and no existing legacy entry field changed. This newer file is the update-comparison baseline; legitimate intervening activity is not mislabeled as update loss.
- Post-fix JVM run passed all 162 tests, zero failures/errors. The large catalog regression took 45 ms on the build machine; this is algorithm coverage, not an Android heatmap-opening benchmark.
- `sauce.bat verify -NoInstall` after the ANR fix: PASS, 893.9 seconds; all isolated migration/backup/profile/Bridge checks, 162 JVM tests, profile APK, vital lint and development signing verification.
- `sauce.bat release -NoInstall`: PASS, 418.3 seconds, release vital lint and isolated regressions. APK v2 signature verified, same official certificate as 1.9; package `com.roinur.saucetracker`, version 2.0/code 14, minimum API 26, target API 34, not debuggable.
- Final APK: `build/releases/2026-10-04/Sauce-Tracker-2.0-release.apk`, 14,390,043 bytes; SHA-256 `944f523134d9616bffd7021cfd8ae48a73bed9c553da16c1d6a2b2d97ca0cc18`; companion checksum file created.
- Installed into the ordinary release package with `adb install -r`; Android reports update time 16:59:27. No uninstall, clear-data or production restore. Debug installation was not changed.
- Pressed the actual dashboard Heatmap widget after installation: Heatmap Overview reached a responsive UI in a 2.28-second tap-plus-uiautomator diagnostic round trip (not a pure rendering benchmark). Back navigation remained responsive. Owner independently confirmed the previously crashing widget now works. Android's last ANR remains the pre-update 16:57:16 event.
- Updated stable Library Health is `Healthy`, 1,935 source entries, zero missing legacy identities and zero orphan profile states. Current and procedural Verified Restore both restored 1,933/1,933 legacy entries in isolation twice, idempotent, zero skipped, lossless source/session/subscription/settings comparison. MangaDex: two entries, two sessions, two resume positions, two chapter-progress rows and zero subscriptions.
- Fresh post-update manual export: `after-final-release-v2.txt`, SHA-256 `7d99ec5fdcd3daa46faea75c55f240ad68f6dd89f13c32f6ae6cfe9ffac96263`. The read-only V2 update checker passed against the newer pre-install baseline: every library/source/personal/history/resume/progress/subscription/preference value is retained. Normal forward profile-use, subscription-check and preference-write timestamps are accepted; changed values still fail. All 15 profile preference values match, with only their recapture timestamps advancing during navigation.
- Original manual 1.9 export SHA-256 remains `b8643ae83f6bc58f56fd64a066e8625adc106443989df60d78b33d4ec8412933`. No private-library screenshot was captured or published during this pass.

## Scope

- Fixed Browser's always-Remove button: absent entries now offer Import through the existing language-aware dialog. Plain import does not set read/rating/pin. Import selects the actual MangaDex language key, and local state refreshes on reader return and language changes.
- Shared chapter-order header in Browser and Library; numeric ascending/descending presentation without changing reader navigation or chapter progress.
- Reused Library's existing Details disclosure for MangaDex; NHentai-only related-gallery navigation remains NHentai-only.
- Added a small Screenshots link into the existing Experimental Gallery, filtered by source/entry/language. New captures persist identity in the existing gallery manifest. No duplicate images or new gallery storage.
- Older captures are matched from their saved title/page name (NHentai) or known chapter-ID prefix (MangaDex). Unknown older associations are not invented; old files are not rewritten.
- Entry-filtered galleries reject invalid scope and obey incognito/App Lock checks at launch/resume. Ordinary Experimental Gallery remains available through its existing route.

## Verification

- Added 8 JVM regression tests for numeric chapter order, stable alternate-group ordering, invalid chapter numbers, source isolation, language isolation and legacy screenshot matching.
- `sauce.bat fast -NoInstall`: PASS (323 seconds), development signature verified by conveyor.
- `sauce.bat verify -NoInstall`: PASS (365.3 seconds): all 152 JVM tests, isolated migration/backup/profile/Bridge regressions, profile APK and vital lint. First attempt stopped on an incorrect 13-character legacy screenshot fixture; corrected to the actual stored 12-character chapter prefix before the passing rerun.
- `sauce.bat release`: PASS (343.7 seconds), isolated regressions rerun and release vital lint passed.
- `apksigner verify --verbose --print-certs`: PASS, APK Signature Scheme v2. Release certificate SHA-256: `3d93101924ef2ce765c5ddfb955dbd688d82691fe31d2a4a2560386d441a9601`.
- `aapt dump badging`: `com.roinur.saucetracker`, versionName `2.0`, versionCode `14`, minimum API 26, target API 34; no `application-debuggable` flag.
- APK: `build/releases/2026-10-04/Sauce-Tracker-2.0-test-release-2026-10-04.apk`, 14,385,927 bytes.
- SHA-256: `602a9e147068fbf9a8d524fcd57bc3a397f2365fe9e498e8d9d6085902b669d8`; companion `.apk.sha256` saved.
- ADB inventory returned no connected device during this pass. Import/language selection, sort gestures, Details expansion, raw capture and filtered-gallery appearance still require real-device QA.

## Data safety

- No APK installed, no application data cleared, no production database opened or changed.
- Release APK requested for local testing, not an announcement that all 2.0 release gates are complete. Debug profiles are not transferred into the release package.

## Later authorized real-device 1.9 upgrade

The sections above describe the earlier offline build pass. The user subsequently explicitly authorized updating the ordinary 1.9 installation after a verified backup.

- Created a new manual V1 export on the phone: `Documents/SauceTracker Backup/sauce_export_20261004_120913.txt`; pulled it into `build/device-qa/release-upgrade-20261004-1209/before-1.9-export.txt`. Original manual exports were not overwritten or rotated.
- Backup SHA-256: `b8643ae83f6bc58f56fd64a066e8625adc106443989df60d78b33d4ec8412933`.
- Verified actual exported content in an isolated in-memory schema-1 database using production migration SQL: 1,927 entries, 570 read, 1,570 pinned, 2,654 tags, 1,368 creators, 857 reading sessions, 120 daily activity rows, 6 subscriptions, 470 seen codes and 173 events. Full restored legacy rows and migrated per-entry personal state matched.
- 1.9 Library Health quick_check returned `ok`. Its existing 60 sessions for removed entries were recorded as a baseline, not deleted. The older rolling backup had 1,931 entries; it was not substituted for the fresh 1,927-entry export.
- Pulled the installed 1.9 APK for evidence; SHA-256 `acc85165ca4eae6fafdc400c52f260dda6a85ea3ef11de1a3e73a36f04733b43`. Its signing certificate matches the 2.0 release certificate.
- Initial `adb install -r` succeeded, but first launch crashed before migration SQL: `preMigrationTables` was initialized after the constructor's database open. Corrected initialization order and added a regression guard. No uninstall, clear-data or restore into production was performed.
- No private-library screenshots were captured; inspection used UI trees and diagnostics.

### Corrected release and on-device result

- `sauce.bat release -NoInstall`: PASS, 416.5 seconds, production SQL/profile/backup/Bridge regressions and release vital lint. Release certificate and APK v2 signature verified before installing.
- Corrected APK: `build/releases/2026-10-04/Sauce-Tracker-2.0-test-release-2026-10-04-migration-fix.apk`; SHA-256 `db5246c8b892ccb46c39b573b1f545855948d608088d8d04a2e265aa7c7cc8aa`. This supersedes the earlier test APK for 1.9 upgrades.
- Installed in place with `adb install -r` into `com.roinur.saucetracker`, versionCode 14 / versionName 2.0. Debug package was not updated. No uninstall or application-data clearing.
- Real first successful migration created exactly the two source-locked profiles, NHentai (`main`) and MangaDex (`mangadex-default`). NHentai is active with all old entries; MangaDex is empty. No visible combined Main profile or debug-profile transfer.
- Fresh manual V2 export: `Documents/SauceTracker Backup/sauce_export_20261004_123018.txt`, local evidence `build/device-qa/release-upgrade-20261004-1209/after-2.0-export.txt`; SHA-256 `436e85a7000066fa27a2b55b0b0ad85e09b3545f653173cf87f8137b4bb0dcb1`.
- `node tools/release-upgrade-backup-check.cjs <before> <after>`: PASS. Every old entry field/tag, creator, session, daily activity row, subscription/seen/event row, popular tag, hidden suggestion and recommendation weight matched; all 31 portable preferences matched. All migrated source metadata/tags and per-profile read/rating/pin/dates also matched. The six subscription `last_checked_at` timestamps moved forward during normal startup checks; this is the only permitted legacy-row difference, explicitly validated as monotonic.
- Phone Library Health: SQLite `quick_check=ok`, 1,927 source entries, zero missing legacy source identities, zero orphan profile states, zero orphan tag relations and valid ratings. All 857 sessions are retained, including the 60 previously removed-entry/browser sessions; zero invalid profile/source history identities. Training feedback and tag presets valid.
- Actual Android Verified Restore (current data) and procedural backup both PASS: 1,927/1,927 entries, zero skipped rows, second isolated restore idempotent, source platform/history/subscriptions and portable settings lossless. Neither restore touched the production database. MangaDex is correctly empty in this installation, so this real-device test does not demonstrate a populated MangaDex export/restore.
- Backup and gallery-download SAF permissions remain persisted. Original manual V1 backup checksum was checked again after the upgrade and is unchanged. Automatic procedural backup updated normally; manual exports were not rotated or replaced.
- Two further process-stop/relaunch cycles reached the dashboard; saved totals remain 1,927 entries, 878 artists, 490 groups, 570 read. No post-fix Sauce Tracker crash in the crash buffer. Debug package remains at its prior 2026-10-03 23:48:04 update time.

## Follow-up: history health, screenshot rows and chapter focus

- Legitimate missing-entry history is classified as `Reading outside library` (including unrated sessions), not a failed backup. Older rows do not invent browser-vs-deletion provenance. Manual/legacy read marks without sessions are informational. Invalid profile/source owners and strict restore comparisons remain blocking checks; no history row or backup format was changed.
- Browser and Library reuse a compact `Saved: Screenshots` metadata link, aligned with creator rows and existing small-shape press/privacy behavior. It still opens the existing scoped Experimental Gallery.
- Library reads resume position and chapter progress together using the explicit active profile. Entry/profile switches create isolated Compose state. A saved chapter is located by exact ID after sorting, centered using measured row/viewport sizes before the list becomes visible, and is not continuously re-centered during manual scrolling. Missing/deleted resume chapters fall back safely to the normal list.
- Added four history-health JVM tests and three resume/centering JVM tests. Extended the isolated SQL diagnostic test to distinguish unrated browser/removed-entry sessions from rated ones while retaining all rows.

### Follow-up verification

- `sauce.bat fast -NoInstall`: PASS (474.9 seconds); debug development signature verified. Installed only `com.roinur.saucetracker.rewrite` in place with `adb install -r`.
- `sauce.bat verify -NoInstall`: PASS (575.4 seconds), including all 159 JVM tests (zero failures/errors), isolated migration/backup/profile/Bridge regressions, profile APK and vital lint. Resume tests cover chapter 150 in a 0–300 list in both directions and exact chapter IDs for alternate groups.
- Actual debug Library Health on device: `Healthy`. All 115 unrated sessions outside the library are retained and informational; 126 manual/older read marks without sessions are informational; no invalid history owners. Strict restore validation was not relaxed.
- Actual Android Verified Restore of current debug data: 1,931/1,931 entries, zero skipped, second restore idempotent and source/history/subscription/settings comparisons lossless. MangaDex includes 11 entries, 3 sessions, 11 reader positions and 16 chapter-progress rows. Existing procedural-backup restore also passed with its own MangaDex entry/session/resume/chapter-progress data. These restores used isolated databases, not production.
- Fresh debug manual export saved separately as `Documents/SauceTracker Backup/sauce_export_20261004_130507.txt`; local evidence under `build/device-qa/history-chapter-focus-20261004-1305/`. Existing manual exports were not replaced.
- Final UI smoke check used the isolated GitHub media database/preferences and privacy masking. The existing Library chapter list rendered, remained manually scrollable and retained its sort control. A selected test chapter could not obtain provider page resources, so a newly generated mid-series bookmark/reopen check could not be completed on-device; the centering/order regression tests passed. No claim of complete on-device centering QA is made.
- Exited GitHub media mode by stopping debug and relaunching its ordinary MainActivity. Stable release was not installed or modified in this follow-up; its package update time remains 2026-10-04 12:27:56. Original verified 1.9 manual backup checksum remains `b8643ae83f6bc58f56fd64a066e8625adc106443989df60d78b33d4ec8412933`.
