# Sauce Tracker build conveyor

Run these commands from the repository root.

## Everyday development

```powershell
.\sauce.bat fast
```

This is the default conveyor: incremental debug build, signature verification,
automatic adb device detection, installation, and launch. It uses the established
JDK 21, Gradle cache, Android debug key, and Android SDK paths. The fast lane keeps
the warm build cache without a persistent Gradle daemon. Compilation uses a 3 GB
heap and one worker so the debug/profile compilers do not exhaust memory together.

## Performance testing

```powershell
.\sauce.bat performance
```

Builds and installs the non-debuggable profile variant used for scrolling, FPS,
startup, and other performance comparisons. It skips release lint; `verify` runs
the complete checks.

## Milestone verification

```powershell
.\sauce.bat verify
```

Runs isolated 2.0 migration and Desktop Bridge regression checks, unit tests and
a full profile build, then installs and launches it. The isolated checks require
Node.js 22.13 or newer with `node:sqlite`; they never connect to a phone or use its
library. Use `-NoInstall` for build-only validation. Real phone/provider QA and
Verified Restore are separate release gates in `TEST_CHECKLIST_2.0.md`.

## Release build

```powershell
.\sauce.bat release
```

Runs the same isolated 2.0 regressions and builds the signed release APK. It never installs it automatically because the
release package is separate from the rewrite development package.

## Useful switches

```powershell
.\sauce.bat fast -NoInstall
.\sauce.bat performance -NoLaunch
.\sauce.bat fast -Online
.\sauce.bat fast -Device "adb-device-serial"
```

Normal builds start from the existing offline dependency cache. If a dependency
is genuinely missing, the script retries once with network access. It does not run
`clean`; generated build output should only be removed when a verified corruption
or stale lock requires it.
