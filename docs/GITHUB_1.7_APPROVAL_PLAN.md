# Sauce Tracker 1.7 GitHub publication record

The project owner approved merging the 1.7 source and README into `main` and publishing the signed APK as a GitHub Release. License selection remains deliberately deferred.

## Proposed repository presentation

- Repository name: `Sauce-Tracker`
- Short description: `A local-first Android library for organizing, exploring, and revisiting gallery metadata.`
- Topics: `android`, `kotlin`, `jetpack-compose`, `library`, `heatmap`, `local-first`, `backup`
- Landing page: the root `README.md` with real privacy-masked screenshots, status badges, release highlights, feature overview, installation, source-build instructions, project structure, privacy/network notes, and release integrity section.
- Changelog: `CHANGELOG.md` as the durable version history.
- Release copy: `release-notes/1.7.0.md` as the GitHub Release description.

## Proposed visual layout

The README uses the supplied privacy-masked screenshots instead of a generated vector hero. The dashboard is the primary overview image, feature rows pair concise explanations with relevant screens, and the complete 15-image set is available in a collapsible gallery. The screenshots contain no performance overlay and obscure private search terms, metadata, and thumbnails.

## Proposed Git history and branches

- Preserve the existing architecture rewrite commits as meaningful rollback points.
- Commit the verified 1.7 implementation and documentation locally.
- Create or rename the publication branch to `main` only after approval.
- Publish the review branch to `Roinur/Sauce-tracker` and merge it into `main`.
- Tag the verified release commit as `v1.7.0`.

No force-push or history rewrite is proposed.

## Proposed GitHub Release

- Tag: `v1.7.0`
- Title: `Sauce Tracker 1.7`
- Description source: `release-notes/1.7.0.md`
- Assets:
  - `Sauce-Tracker-1.7-release.apk`
  - `Sauce-Tracker-1.7-release.apk.sha256`
- Mark as latest stable release, not a prerelease.
- Let GitHub generate source archives from the tag.

## Repository hygiene before upload

- Keep `signing.properties`, keystores, local Android paths, Gradle homes, build folders, APKs, and logs ignored.
- Do not upload the development profile/debug APK.
- Verify the release APK signature, version name/code, package ID, and SHA-256 checksum.
- Review the public diff for credentials and private paths before the first push.
- Enable GitHub secret scanning and dependency alerts where available.

## License decision deferred

No new license is selected in the README-preview change. Choose one before treating the source as generally reusable:

- **GPL-3.0-only**: recommended when derivatives should remain open source.
- **Apache-2.0**: recommended when permissive reuse is preferred.
- **No public source license / private repository**: appropriate when GitHub is only used for controlled source hosting and release distribution.

The README currently states that no redistribution rights are granted until this decision is approved.

## Approval gate

Publication checklist:

- [x] GitHub owner and repository: `Roinur/Sauce-tracker`.
- [x] Public repository.
- [ ] License choice (deliberately deferred).
- [x] Publish the existing architecture commit history as-is.
- [x] Final supplied screenshot set; generated vector hero removed.
- [x] Release notes and exact APK/checksum assets.
- [x] Permission to set `origin` and push the review branch.
- [x] Permission to merge, create `v1.7.0`, and upload release assets.

## Locally verified release candidate

- Package: `com.example.saucetracker`
- Version: `1.7` (`versionCode 11`)
- Minimum Android: API 26
- Target/compile SDK: API 34
- App label: `Sauce Tracker`
- APK size: 9,645,245 bytes
- SHA-256: `B2061A18BABE884EBC1EABD69925273970E91421761AB76BE73012F21F56550F`
- APK Signature Scheme v2: verified
- Signing certificate SHA-256: `3d93101924ef2ce765c5ddfb955dbd688d82691fe31d2a4a2560386d441a9601`
- Signing certificate matches the previous Sauce Tracker release APK.
- Git remote: `https://github.com/Roinur/Sauce-tracker.git`.
