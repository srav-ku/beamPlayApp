param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$adb  = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$apk  = Join-Path $root "composeApp\build\outputs\apk\debug\composeApp-debug.apk"
$pkg  = "app.cinephile"
$wmemo = Join-Path $root ".beam-wireless"

if (-not (Test-Path $adb)) { Write-Host "adb not found at: $adb" -ForegroundColor Red; exit 1 }

function Reset-Adb {
    & $adb kill-server 2>&1 | Out-Null
    Start-Sleep -Seconds 2
    & $adb start-server 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}

function Live-Device {
    @(& $adb devices -l) | Select-Object -Skip 1 |
        Where-Object { $_ -match '\sdevice\b' } | Select-Object -First 1
}

function Install-Apk($serial) {
    Write-Host "Installing (streamed)..." -ForegroundColor Cyan
    & $adb -s $serial install -r $apk
    if ($LASTEXITCODE -eq 0) { return $true }

    Write-Host "Retrying with push + on-device install..." -ForegroundColor Yellow
    & $adb -s $serial push $apk /data/local/tmp/beam.apk | Out-Null
    & $adb -s $serial shell pm install -r /data/local/tmp/beam.apk
    & $adb -s $serial shell rm -f /data/local/tmp/beam.apk | Out-Null
    if ($LASTEXITCODE -eq 0) { return $true }

    Write-Host "Retrying with a clean install..." -ForegroundColor Yellow
    & $adb -s $serial uninstall $pkg | Out-Null
    Start-Sleep -Seconds 2
    & $adb -s $serial install $apk
    return ($LASTEXITCODE -eq 0)
}

function Save-Target($addr) { Set-Content -Path $wmemo -Value $addr -Encoding ascii }

Reset-Adb

Write-Host ""
Write-Host "=== Cinephile installer ===" -ForegroundColor Cyan

# ---- 1. already reachable (USB or a remembered Wi-Fi target)? ----
$serial = $null
$live = Live-Device
if ($live) {
    $serial = ($live -split '\s+')[0]
    Write-Host "Device ready: $serial" -ForegroundColor Green
} elseif (Test-Path $wmemo) {
    $target = (Get-Content $wmemo -Raw).Trim()
    Write-Host "Trying remembered Wi-Fi target $target ..." -ForegroundColor Cyan
    & $adb connect $target 2>&1 | Out-Null
    Start-Sleep -Seconds 3
    $live = Live-Device
    if ($live) { $serial = ($live -split '\s+')[0]; Write-Host "Connected: $serial" -ForegroundColor Green }
}

# ---- 2. nothing reachable: offer the one-command Wi-Fi pairing flow ----
if (-not $serial) {
    Write-Host ""
    Write-Host "No phone reachable yet." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "On the phone: Developer options > Wireless debugging > ON" -ForegroundColor White
    Write-Host "Then tap 'Pair device with pairing code' - it shows an IP:PORT and a 6-digit code." -ForegroundColor White
    Write-Host ""
    $addr = Read-Host "Paste the IP:PORT shown on that pairing screen (or just press Enter for USB)"
    $addr = $addr.Trim()

    if ($addr) {
        $code = (Read-Host "Enter the 6-digit pairing code").Trim()
        Write-Host "Pairing with $addr ..." -ForegroundColor Cyan
        & $adb pair $addr $code
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Pairing failed. Make sure the pairing screen is still open and the code/port are current." -ForegroundColor Red
            exit 1
        }
        # after pairing, try to connect on the same host (Android usually accepts it)
        $hostOnly = ($addr -split ':')[0]
        & $adb connect $addr 2>&1 | Out-Null
        Start-Sleep -Seconds 3
        $live = Live-Device
        if (-not $live) {
            # fall back to the classic Wi-Fi port
            Write-Host "Trying $hostOnly`:5555 ..." -ForegroundColor Cyan
            & $adb connect "$hostOnly`:5555" 2>&1 | Out-Null
            Start-Sleep -Seconds 3
            $live = Live-Device
        }
        if (-not $live) {
            Write-Host ""
            Write-Host "Paired, but could not open a connection automatically." -ForegroundColor Yellow
            Write-Host "On the phone go back to the Wireless debugging screen and copy the" -ForegroundColor White
            Write-Host "'IP address & Port' (a different port), then re-run this script." -ForegroundColor White
            exit 1
        }
        $serial = ($live -split '\s+')[0]
        Save-Target $serial
        Write-Host "Wireless connection established: $serial (remembered)" -ForegroundColor Green
    }
}

if (-not $serial) {
    Write-Host ""
    Write-Host "No device. For USB: plug in, accept 'Allow USB debugging', re-run this script." -ForegroundColor Yellow
    & $adb devices -l
    exit 1
}

# ---- 3. build (optional) then install ----
if (-not $SkipBuild) {
    Write-Host "Building debug APK..." -ForegroundColor Cyan
    & (Join-Path $root "gradlew.bat") :composeApp:assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { Write-Host "Build failed - nothing installed." -ForegroundColor Red; exit 1 }
}

if (-not (Test-Path $apk)) { Write-Host "APK not found: $apk" -ForegroundColor Red; exit 1 }

if (Install-Apk $serial) {
    Write-Host "Installed. Launching Cinephile..." -ForegroundColor Green
    & $adb -s $serial shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Out-Null
} else {
    Write-Host "Install failed - re-run the script." -ForegroundColor Red
    exit 1
}
