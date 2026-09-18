# wireless-adb.ps1 - connect this PC to the phone over Wi-Fi, no cable.
#
# Two ways, in order of reliability:
#   A) Android 11+ "Wireless debugging" pairing (recommended, works without USB)
#   B) classic "adb tcpip 5555" (needs USB once; some phones block it)

param(
    [string]$LastIp = "192.168.1.39",
    [switch]$Pair
)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

Write-Host "=== Cinephile wireless ADB ===" -ForegroundColor Cyan

if ($Pair) {
    Write-Host ""
    Write-Host "On the phone: Settings > Developer options > Wireless debugging > Pair device with pairing code" -ForegroundColor Yellow
    Write-Host "It shows:  IP address & Port  +  a 6-digit code" -ForegroundColor Yellow
    Write-Host ""
    $ipPort = Read-Host "Paste the IP:PORT shown under 'Pair device with pairing code'"
    $code   = Read-Host "Enter the 6-digit pairing code"
    & $adb pair $ipPort $code
    Write-Host ""
    Write-Host "Now go back one screen and read the IP:PORT at the top of 'Wireless debugging'" -ForegroundColor Yellow
    $conn = Read-Host "Paste that IP:PORT (the main one, not the pairing one)"
    & $adb connect $conn
} else {
    Write-Host "Trying remembered address $LastIp:5555 ..."
    & $adb connect "$LastIp`:5555"
}

Start-Sleep 2
Write-Host ""
& $adb devices
Write-Host ""
Write-Host "If the list shows your device as 'device', you are connected." -ForegroundColor Green
Write-Host "If it shows nothing, run:  .\wireless-adb.ps1 -Pair" -ForegroundColor Yellow