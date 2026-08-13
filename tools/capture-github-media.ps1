[CmdletBinding()]
param(
    [string]$Device,
    [string]$Name = ("github-media-{0:yyyyMMdd-HHmmss}.png" -f (Get-Date))
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$adb = 'C:\Users\roinu\stock-tracker\android-app\android-sdk\platform-tools\adb.exe'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$reviewRoot = Join-Path $repoRoot 'build\github-media-review'
$outputPath = [IO.Path]::GetFullPath((Join-Path $reviewRoot $Name))

if (-not $outputPath.StartsWith($reviewRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Review captures must remain inside build/github-media-review.'
}
if ([IO.Path]::GetExtension($outputPath) -ne '.png') {
    throw 'GitHub media captures must use the .png extension.'
}

if (-not $Device) {
    $deviceLines = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "`tdevice$" })
    if ($deviceLines.Count -ne 1) {
        throw "Expected one authorized adb device, found $($deviceLines.Count). Use -Device when needed."
    }
    $Device = $deviceLines[0] -replace "`tdevice$", ''
}

$activities = (& $adb -s $Device shell dumpsys activity activities) -join "`n"
$topMatch = [regex]::Match(
    $activities,
    'topResumedActivity=.*com\.example\.saucetracker\.rewrite/[^\s}]+\s+t(\d+)'
)
if (-not $topMatch.Success) {
    throw 'GitHub media mode is not the foreground activity. Capture refused.'
}
$taskId = [regex]::Escape($topMatch.Groups[1].Value)
$githubLauncherInForegroundTask =
    $activities -match "com\.example\.saucetracker\.rewrite/com\.example\.saucetracker\.app\.GitHubMediaLauncher\s+t$taskId}"
if (-not $githubLauncherInForegroundTask) {
    throw 'The foreground Sauce Tracker task was not launched in GitHub media mode. Capture refused.'
}

New-Item -ItemType Directory -Path $reviewRoot -Force | Out-Null
$remotePath = '/sdcard/sauce-github-media-review.png'
$rawPath = Join-Path $reviewRoot '.raw-github-media.png'

try {
    & $adb -s $Device shell screencap -p $remotePath
    if ($LASTEXITCODE -ne 0) { throw 'ADB screencap failed.' }
    & $adb -s $Device pull $remotePath $rawPath | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not pull the ADB capture.' }

    Add-Type -AssemblyName System.Drawing
    $source = [Drawing.Bitmap]::FromFile($rawPath)
    try {
        $windowDump = (& $adb -s $Device shell dumpsys window) -join "`n"
        $topCrop = 0
        $bottomCrop = 0

        $statusMatches = [regex]::Matches(
            $windowDump,
            'type=statusBars frame=\[0,0\]\[(\d+),(\d+)\] visible=true'
        )
        foreach ($match in $statusMatches) {
            if ([int]$match.Groups[1].Value -eq $source.Width) {
                $topCrop = [Math]::Max($topCrop, [int]$match.Groups[2].Value)
            }
        }

        $navigationMatches = [regex]::Matches(
            $windowDump,
            'type=navigationBars frame=\[0,(\d+)\]\[(\d+),(\d+)\] visible=true'
        )
        foreach ($match in $navigationMatches) {
            $navTop = [int]$match.Groups[1].Value
            $navWidth = [int]$match.Groups[2].Value
            $navBottom = [int]$match.Groups[3].Value
            if ($navWidth -eq $source.Width -and $navBottom -eq $source.Height -and $navTop -gt 0) {
                $bottomCrop = [Math]::Max($bottomCrop, $source.Height - $navTop)
            }
        }

        if ($topCrop -le 0) { throw 'Could not determine the Android status-bar inset.' }
        $captureHeight = $source.Height - $topCrop - $bottomCrop
        if ($captureHeight -le 0) { throw 'Calculated capture bounds are invalid.' }

        $bounds = [Drawing.Rectangle]::new(0, $topCrop, $source.Width, $captureHeight)
        $cropped = $source.Clone($bounds, $source.PixelFormat)
        try {
            $cropped.Save($outputPath, [Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $cropped.Dispose()
        }
    } finally {
        $source.Dispose()
    }
} finally {
    & $adb -s $Device shell rm -f $remotePath | Out-Null
    Remove-Item -LiteralPath $rawPath -Force -ErrorAction SilentlyContinue
}

Write-Host "Private review capture saved: $outputPath" -ForegroundColor Green
Write-Host 'The status/navigation bars were excluded. This file is below build and is not ready for GitHub publication.'
