[CmdletBinding()]
param(
    [ValidateSet('fast', 'performance', 'verify', 'release')]
    [string]$Mode = 'fast',
    [switch]$NoInstall,
    [switch]$NoLaunch,
    [switch]$Online,
    [string]$Device
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkRoot = 'C:\Users\roinu\stock-tracker\android-app\tools\jdk-21.0.10+7'
$androidSdk = 'C:\Users\roinu\stock-tracker\android-app\android-sdk'
$adb = Join-Path $androidSdk 'platform-tools\adb.exe'
$androidUserHome = 'C:\Users\roinu\.android'
$gradleHome = Join-Path $projectRoot '.gradle-user-home'

$requiredPaths = @(
    (Join-Path $jdkRoot 'bin\java.exe'),
    $adb,
    (Join-Path $projectRoot 'gradlew.bat'),
    (Join-Path $androidUserHome 'debug.keystore')
)
foreach ($requiredPath in $requiredPaths) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required build tool is missing: $requiredPath"
    }
}

New-Item -ItemType Directory -Path $gradleHome -Force | Out-Null
$logDirectory = Join-Path $projectRoot 'build\quick-build-logs'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$logPath = Join-Path $logDirectory "last-$Mode.log"

$env:JAVA_HOME = $jdkRoot
$env:Path = "$(Join-Path $jdkRoot 'bin');$env:Path"
$env:ANDROID_SDK_ROOT = $androidSdk
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_USER_HOME = $androidUserHome
$env:GRADLE_USER_HOME = $gradleHome

$gradleTask = switch ($Mode) {
    'fast' { ':app:assembleDebug' }
    'performance' { ':app:assembleProfile' }
    'verify' { ':app:testDebugUnitTest', ':app:assembleProfile' }
    'release' { ':app:assembleRelease' }
}
$apkPath = switch ($Mode) {
    'fast' { Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk' }
    'performance' { Join-Path $projectRoot 'app\build\outputs\apk\profile\app-profile.apk' }
    'verify' { Join-Path $projectRoot 'app\build\outputs\apk\profile\app-profile.apk' }
    'release' { Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk' }
}
$installable = $Mode -in @('fast', 'performance', 'verify')

function Invoke-GradleBuild {
    param([bool]$UseOffline)

    $daemonMode = if ($Mode -in @('fast', 'performance')) { '--daemon' } else { '--no-daemon' }
    $arguments = @(
        $daemonMode,
        '--build-cache'
    )
    if ($UseOffline) {
        $arguments += '--offline'
    }
    $arguments += $gradleTask
    if ($Mode -eq 'performance') {
        # Performance APKs should behave like profile builds, but lint belongs in verify/release.
        $arguments += @(
            '-x', 'lintVitalProfile',
            '-x', 'lintVitalAnalyzeProfile',
            '-x', 'lintVitalReportProfile'
        )
    }

    Write-Host "`nBuilding '$Mode' ($(if ($UseOffline) { 'offline cache' } else { 'network allowed' }))..." -ForegroundColor Cyan
    $stdoutPath = "$logPath.stdout"
    $stderrPath = "$logPath.stderr"
    $gradleWrapper = Join-Path $projectRoot 'gradlew.bat'
    $commandLine = "`"$gradleWrapper`" $($arguments -join ' ') 1>`"$stdoutPath`" 2>`"$stderrPath`""
    Push-Location $projectRoot
    try {
        # Redirect inside cmd.exe so a persistent Gradle daemon cannot inherit
        # and keep PowerShell's output pipe open after the wrapper has exited.
        & $env:ComSpec /d /s /c $commandLine
        $gradleExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    $buildOutput = @(
        if (Test-Path -LiteralPath $stdoutPath) { Get-Content -LiteralPath $stdoutPath }
        if (Test-Path -LiteralPath $stderrPath) { Get-Content -LiteralPath $stderrPath }
    )
    $buildOutput | Set-Content -LiteralPath $logPath
    $buildOutput | ForEach-Object { Write-Host $_ }
    return $gradleExitCode
}

function Test-IsDependencyCacheFailure {
    if (-not (Test-Path -LiteralPath $logPath)) {
        return $false
    }
    $logText = Get-Content -LiteralPath $logPath -Raw
    return $logText -match 'offline mode|No cached version|Could not resolve|Could not GET|Could not HEAD'
}

function Get-ApkSigner {
    $buildToolsRoot = Join-Path $androidSdk 'build-tools'
    $candidate = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
    if (-not $candidate) {
        throw "apksigner.bat was not found below $buildToolsRoot"
    }
    return $candidate
}

function Assert-DevelopmentSignature {
    param([string]$Path)

    $expectedSha256 = '28b950f51c39412f152458ba9cc5dadd0c2095a33ba1e30f8cfcdb94f0d37856'
    $signerOutput = & (Get-ApkSigner) verify --print-certs $Path 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "APK signature verification failed:`n$($signerOutput -join "`n")"
    }
    $digestLine = $signerOutput | Where-Object { $_ -match 'SHA-256 digest:\s*([0-9a-fA-F]+)' } | Select-Object -First 1
    if (-not $digestLine -or $digestLine -notmatch 'SHA-256 digest:\s*([0-9a-fA-F]+)') {
        throw 'Could not read the APK signing certificate fingerprint.'
    }
    if ($Matches[1].ToLowerInvariant() -ne $expectedSha256) {
        throw "Wrong development signing certificate. Refusing to replace the installed app. Actual: $($Matches[1])"
    }
}

function Resolve-AdbDevice {
    if ($Device) {
        return $Device
    }

    $connected = @(
        & $adb devices |
            Select-Object -Skip 1 |
            ForEach-Object {
                if ($_ -match '^(?<serial>.+?)\s+device(?:\s|$)') {
                    $Matches.serial
                }
            }
    )
    if ($connected.Count -eq 0) {
        throw 'No authorized adb device is connected. Enable Wireless debugging and approve this computer if Android asks.'
    }
    if ($connected.Count -gt 1) {
        throw "More than one adb device is connected. Re-run with -Device '<serial>'. Devices: $($connected -join ', ')"
    }
    return $connected[0]
}

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
Write-Host 'Sauce Tracker build conveyor' -ForegroundColor Magenta
Write-Host "Mode:          $Mode"
Write-Host "JDK:           $jdkRoot"
Write-Host "Gradle cache:  $gradleHome"
Write-Host "Android home:  $androidUserHome"

$offlineFirst = -not $Online
$exitCode = Invoke-GradleBuild -UseOffline $offlineFirst
if ($exitCode -ne 0 -and $offlineFirst -and (Test-IsDependencyCacheFailure)) {
    Write-Host "`nA dependency is missing from the warm cache. Retrying once with network access..." -ForegroundColor Yellow
    $exitCode = Invoke-GradleBuild -UseOffline $false
}
if ($exitCode -ne 0) {
    throw "Gradle failed with exit code $exitCode. Full log: $logPath"
}
if (-not (Test-Path -LiteralPath $apkPath -PathType Leaf)) {
    throw "Gradle succeeded but the expected APK is missing: $apkPath"
}

if ($installable) {
    Assert-DevelopmentSignature -Path $apkPath
}

if ($installable -and -not $NoInstall) {
    $serial = Resolve-AdbDevice
    $rotationBefore = (& $adb -s $serial shell settings get system accelerometer_rotation).Trim()
    try {
        Write-Host "`nInstalling on: $serial" -ForegroundColor Cyan
        & $adb -s $serial install -r $apkPath
        if ($LASTEXITCODE -ne 0) {
            throw 'adb install failed.'
        }

        if (-not $NoLaunch) {
            $previousErrorAction = $ErrorActionPreference
            try {
                # Android's monkey command prints normal launch diagnostics to stderr.
                $ErrorActionPreference = 'Continue'
                & $adb -s $serial shell monkey -p com.example.saucetracker.rewrite -c android.intent.category.LAUNCHER 1 2>&1 | Out-Null
                $launchExitCode = $LASTEXITCODE
            } finally {
                $ErrorActionPreference = $previousErrorAction
            }
            if ($launchExitCode -ne 0) {
                throw 'The APK installed, but launching the app failed.'
            }
        }
    } finally {
        # Some adb-driven QA flows temporarily enable auto-rotation. Never let a
        # build/install run change the user's system-wide rotation preference.
        $rotationAfter = (& $adb -s $serial shell settings get system accelerometer_rotation).Trim()
        if ($rotationBefore -in @('0', '1') -and $rotationAfter -ne $rotationBefore) {
            & $adb -s $serial shell settings put system accelerometer_rotation $rotationBefore
        }
    }
}

$stopwatch.Stop()
Write-Host "`nSUCCESS in $([math]::Round($stopwatch.Elapsed.TotalSeconds, 1)) seconds" -ForegroundColor Green
Write-Host "APK: $apkPath"
Write-Host "Log: $logPath"
