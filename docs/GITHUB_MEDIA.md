# GitHub media mode

GitHub media mode opens the real Sauce Tracker UI in an isolated secondary process. It copies the current library into a separate app-internal database and reads that copy for the session. The production database is never replaced or written, WorkManager is not started in the media process, and procedural backups are disabled there.

For an in-app toggle, enter `github` in Search everything and press Open in browser. Repeat the same action to leave GitHub media mode and return to the normal app process. ADB remains the preferred route for repeatable screenshot sessions.

Install a current debug or profile build, connect one authorized ADB device, then run:

```powershell
.\tools\github-media.ps1
```

A custom JSON file can select the initial `surface`, `theme`, privacy masking, and real incognito state:

```json
{
  "surface": "entries",
  "theme": "light",
  "privacyMask": true,
  "incognito": false
}
```

Supported surfaces are `dashboard`, `entries`, `tags`, `suggestions`, `subscriptions`, `creators`, `heatmap`, and `history`. Themes are `system`, `light`, and `dark`. Set `incognito` to `true` to capture the complete incognito palette and behavior immediately.

GitHub media mode requires `privacyMask` to remain enabled. The same obfuscation boundaries used by incognito mode are routed over thumbnails and sensitive text without enabling incognito itself, but the media process applies a substantially stronger blur and overlay. This stronger treatment also applies when real incognito is enabled inside the media process. The normal app process and its private-use incognito appearance are unchanged.

Theme and accent controls remain available. The media process also uses a separate copy of app preferences, so changing light or dark mode for screenshots does not change the normal app.

The normal incognito toggle remains independent. Turning it on still enables the complete incognito behavior and palette; turning it off returns to the selected light or dark palette while the GitHub privacy mask remains active.

```powershell
.\tools\github-media.ps1 -Json C:\path\github-media.json
```

Leaving media mode through the hidden `github` action returns to the normal process and production database. Force-stopping the media process and opening Sauce Tracker normally does the same.

Capture a review image without Android status or navigation bars:

```powershell
.\tools\capture-github-media.ps1
```

Review captures are written only below `build/github-media-review`, which is ignored by Git. Move an approved image into `docs/screenshots` only as a separate, explicit publication step.
