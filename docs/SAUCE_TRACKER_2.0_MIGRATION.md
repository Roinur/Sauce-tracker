# Sauce Tracker 2.0 migration and recovery

The in-place upgrade keeps package identity `com.roinur.saucetracker`. Before schema 1 is changed, the app writes an internal `sauce-tracker-before-v2.json` snapshot and compares every preserved table against the still-open 1.9 database. An existing snapshot is checked the same way before reuse. If it is stale or incomplete, the file is retained and a new validated snapshot gets a unique name; the stale file is never trusted as a current recovery point. The SQLite upgrade then runs transactionally, maps every legacy code to `nhentai:<code>`, creates two source-locked profiles named NHentai and MangaDex, activates NHentai with all existing data, and validates entry counts, parents and profile state. A failed check throws from `onUpgrade`, allowing SQLite to roll the transaction back instead of opening an empty database.

Fresh 2.0 installs use the same two locked defaults (both initially empty). No visible Main or combined profile is created; mixed-source profiles are user-created. The NHentai default retains the internal `main` database key to preserve existing references. Debug (`com.roinur.saucetracker.rewrite`) and release use separate app storage: debug profiles are not automatically transferred to release. Existing 2.0 development profiles are preserved in place rather than destructively reset, and deleted default profiles are not recreated on every startup.

Do not uninstall 1.9 as a recovery step. Retry the upgrade or export diagnostics and the retained pre-migration snapshot. User-selected document folders and manual exports are not modified by migration or rolling-backup rotation.

V2 restore previews must be treated as source/profile data. V1 files remain accepted and always restore entries, personal state, history, subscriptions and profile preferences into the NHentai default profile, even when MangaDex is active; after success NHentai is selected. V2 retains full or explicitly selected profile restore behavior. Verified Restore imports twice into an isolated database and compares the resulting snapshot to confirm idempotence before production data is considered recoverable.

Schema 8 also repairs missing or malformed legacy alternate-title JSON in earlier
2.0 development databases. Valid fetched title variants are retained. Titles with
quotes, backslashes and multiline text use JSON serialization rather than SQL
string concatenation.

`sauce.bat verify -NoInstall` runs production migration SQL against disposable
empty, one-entry and 2,500-entry 1.9 fixtures, including interruption, rollback,
retry and repeated setup. JVM tests cover the Kotlin title serializer. These
checks are not a substitute for running actual Android Verified Restore with a
mixed NHentai/MangaDex export; the remaining gates are in `TEST_CHECKLIST_2.0.md`.

Source-platform backup export and restore share one column contract. Import
preflight rejects unsupported versions, missing arrays, duplicate identities,
broken profile/source/entry parents, invalid alternate-title JSON and invalid
reader progress before a database transaction starts. Earlier V5 payloads without
`completed_at` remain accepted. Restoring one selected profile scopes history and
subscription replacement to that profile, not whichever profile was active.

The local source-backup test covers every persisted source-platform column using
the production SQLite schema and a synthetic mixed-provider fixture. It checks
language variants, independent profile status/progress, preferences, cover URLs,
chapter counts, repeated restore and rollback; the Android import/SAF/media
pipeline still requires a separate device test.

History may include browser-only or removed entries; those sessions are retained
and do not require current library metadata. Their profile and source owners must
still exist. A file containing multi-source/profile history but no source-platform
payload is incomplete and is rejected before restore, rather than silently
dropping MangaDex metadata or attaching history to nonexistent profiles. Keep the
original file and use a complete current V2 export instead. Genuine V1 NHentai
files remain supported; isolated round-trip comparisons add only the defined new
field defaults and still detect changed values or missing rows.

Library Health only reads existing document folders/backups. It does not create
a folder, rewrite `.nomedia`, rotate a backup or delete reading history while
scanning.
