# Sauce Tracker 2.0 — navigation maintenance verification

## Delivered changes

- Dashboard resume compares SQLite `data_version` and `total_changes()` on the same
  live helper; unchanged state is retained, while unknown tokens or real writes refresh.
- Tag/creator visibility uses linear indexed lookup and reuses fetched profile state.
- Compatibility initialization is gated per database path/version/process, with
  invalidation on create/upgrade, retry after failure and isolated-restore checks.
- NHentai Browser lazily initializes the MangaDex chapter-cache database.
- Automatic backup preference capture writes actual changes and removes deleted keys;
  unchanged preference rows/timestamps are not rewritten.

## Build and regression evidence

- Post-change JVM result files contain 176 tests, zero failures and zero errors.
- `sauce.bat verify -NoInstall` passed after the implementation: isolated 2.0
  regressions, JVM tests, profile APK and lint; conveyor elapsed time 528.1 seconds.
- `sauce.bat release -NoInstall` passed: signed release APK, release lint and isolated
  regressions; conveyor elapsed time 401.7 seconds.
- Before publication, `node tools/verify-v2.cjs` was rerun successfully, including
  migration interruption/rollback/retry, source backup, profile transfer, navigation
  tokens, GitHub media isolation and Bridge library/reader/privacy tests.
- APK signature verification passed with the existing official certificate SHA-256:
  `3d93101924ef2ce765c5ddfb955dbd688d82691fe31d2a4a2560386d441a9601`.
- Package `com.roinur.saucetracker`, version 2.0/code 14, minimum API 26, target API 34;
  release APK is not debuggable.

## Actual stable update and preserved data

- A fresh procedural V2 export was pulled and validated before installation. Its
  source/profile payload restored twice in an isolated SQLite database with identical
  canonical data, valid ownership, foreign keys and integrity.
- Installed the signed release in place using `adb install -r`, then brought the
  ordinary stable activity to the foreground. No uninstall, clear-data or production
  restore was performed; the separate debug package was not changed.
- A fresh post-update procedural export passed
  `tools/v2-update-backup-check.cjs` against the pre-update export. Library, personal
  profile state, history, subscriptions, preferences, MangaDex resume and chapter
  progress were retained. Only defined monotonic operational timestamps may differ.
- The post-update source/profile payload also restored twice in isolation without
  changing production data.
- The owner reported markedly faster local navigation after installation. No
  controlled before/after phone timing benchmark is claimed.

## Published artifact

- File: `Sauce-Tracker-2.0-release.apk`; 14,393,232 bytes.
- SHA-256: `c56947970db21e7928a4f7925d898cbb1b2ed4a66bf881500fa107861cc7f4c6`.
- This is the exact APK installed and tested, not a different rebuild made during
  publication. Publication changes only source documentation, tags and release assets.
- The original `v2.0.0` source tag is retained; `v2.0.0-navigation` identifies the
  source for the refreshed binary. The existing 2.0 release/download URL is retained.
- Existing provider/device/QR/browser manual coverage caveats remain in
  `TEST_CHECKLIST_2.0.md`; maintenance checks do not imply exhaustive new provider QA.
