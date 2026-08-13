# Sauce Tracker build conveyor

Run these commands from the repository root.

## Everyday development

```powershell
.\sauce.bat fast
```

This is the default conveyor: incremental debug build, signature verification,
automatic adb device detection, installation, and launch. It uses the established
JDK 21, Gradle cache, Android debug key, and Android SDK paths. The fast lane keeps
a Gradle daemon warm between runs; the heavier lanes use isolated processes.

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

Runs unit tests and a full profile build, then installs and launches it.

## Release build

```powershell
.\sauce.bat release
```

Builds the signed release APK. It never installs it automatically because the
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
