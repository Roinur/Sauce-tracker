[CmdletBinding()]
param(
    [string]$Json = (Join-Path $PSScriptRoot '..\docs\github-media.sample.json'),
    [string]$Device
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$adb = 'C:\Users\roinu\stock-tracker\android-app\android-sdk\platform-tools\adb.exe'
$jsonPath = (Resolve-Path -LiteralPath $Json).Path
$raw = Get-Content -LiteralPath $jsonPath -Raw -Encoding UTF8
[void](ConvertFrom-Json -InputObject $raw -ErrorAction Stop)
$bytes = [Text.Encoding]::UTF8.GetBytes($raw)
if ($bytes.Length -gt 48KB) { throw 'GitHub media config must be 48 KB or smaller.' }
$payload = [Convert]::ToBase64String($bytes)

if (-not $Device) {
    $deviceLines = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "`tdevice$" })
    if ($deviceLines.Count -ne 1) {
        throw "Expected one authorized adb device, found $($deviceLines.Count). Use -Device when needed."
    }
    $Device = $deviceLines[0] -replace "`tdevice$", ''
}

$rotationBefore = (& $adb -s $Device shell settings get system accelerometer_rotation).Trim()
try {
    & $adb -s $Device shell am start -S `
        -n 'com.example.saucetracker.rewrite/com.example.saucetracker.app.GitHubMediaLauncher' `
        --es github_media_config_b64 $payload
    if ($LASTEXITCODE -ne 0) { throw 'Could not open the app in GitHub media mode.' }
} finally {
    $rotationAfter = (& $adb -s $Device shell settings get system accelerometer_rotation).Trim()
    if ($rotationBefore -in @('0', '1') -and $rotationAfter -ne $rotationBefore) {
        & $adb -s $Device shell settings put system accelerometer_rotation $rotationBefore
    }
}

Write-Host "Loaded GitHub media config into the real Sauce Tracker UI: $jsonPath" -ForegroundColor Green
Write-Host 'The session uses a separate database copy and cannot overwrite the library or rolling backups.'
