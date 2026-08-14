# Sauce Tracker 1.8 release checklist

## Upgrade and startup

- Install the signed 1.8 APK over 1.7 with `adb install -r`.
- Confirm library data, ratings, read state, subscriptions, downloads, settings, and backups remain present.
- Launch from the icon and from Recents; confirm the active Browser/app task is preserved as expected.

## Dashboard and Suggested Entries

- Confirm the dashboard fits the device without unexpected empty space or required scrolling.
- Swipe between Suggested, Random, and Sauce Finder.
- Open Suggested Entries, restart the app, and confirm dashboard preview thumbnails return from cache.
- Tap a Suggested preview and confirm the Suggested page scrolls to that exact entry.
- Change suggestion modes/weights and confirm the current list remains visible while refreshing.

## Sauce Finder

- Pick an image and share an image into Sauce Tracker.
- Build, pause, and resume the index; confirm completed hashes are not rebuilt.
- Confirm image count, entry count, index size, and rounded progress display correctly.
- Match a full page and a reasonable crop, then open the returned entry.
- Confirm incognito/GitHub privacy treatment obscures sensitive match details.

## Browser and Library

- Open a local entry in Browser, switch apps, return, close Browser, and confirm Library entries remain visible.
- Confirm website More like this cards show titles and open correctly.
- Confirm comments show author and message rather than the author twice.
- Confirm tag counts appear once.
- Confirm Browser detail titles are obscured in incognito.
- Test Parts across active search/tag/read filters; test More like this and Same artist under Read and Unread filters.

## Heatmap and subscriptions

- Open Tag and Entry Heatmaps and verify pan, zoom, selection, family outlines, and cached layout.
- Test 10%, 25%, 50%, and 100% thumbnail zones on a large cached Entry Heatmap.
- Confirm already imported galleries do not appear in subscription updates or notification totals.
- Confirm complete backup/export still retains subscription event history.

## Release integrity

- Run `./sauce.bat verify`.
- Run `./sauce.bat release`.
- Verify APK signing, versionName 1.8, versionCode 12, SHA-256 file, GitHub Release assets, and public README links.
