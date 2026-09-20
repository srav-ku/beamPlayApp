# enrich-catalog.ps1 - fill Rotten Tomatoes / Metacritic / box office / trailer
# for every movie in the catalog.
#
# RUN IT FROM YOUR PC (PowerShell), not from a browser:
#     cd C:\Users\srava\Desktop\beamPlayApp
#     .\enrich-catalog.ps1 -Password "<your ADMIN_PASSWORD from worker line 31>"
#
# It logs in as the admin account, walks the catalog page by page and calls the
# worker's /admin/enrich endpoint in small batches. Safe to re-run: the worker
# uses COALESCE, so it only fills what is missing and never blanks a value.

param(
    [string]$Base     = "https://beamplay.beam-api.workers.dev",
    [string]$Email    = "chanducharan2030@gmail.com",
    [string]$Password = "",
    [int]   $Batch    = 20,     # ids per /admin/enrich call (worker caps at 50)
    [int]   $MaxPages = 40      # 40 pages x 50 rows = up to 2000 movies
)

if ([string]::IsNullOrWhiteSpace($Password)) {
    Write-Host "Pass your admin password:  .\enrich-catalog.ps1 -Password `"....`"" -ForegroundColor Yellow
    exit 1
}

Write-Host "Logging in as $Email ..."
try {
    $login = Invoke-RestMethod -Method Post -Uri "$Base/auth/login" `
        -ContentType "application/json" `
        -Body (@{ email = $Email; password = $Password } | ConvertTo-Json) -TimeoutSec 60
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

if (-not $login.token) {
    Write-Host "Login returned no token - check the password." -ForegroundColor Red
    exit 1
}

$headers = @{ Authorization = "Bearer $($login.token)" }
$total   = 0
$failed  = 0

for ($page = 1; $page -le $MaxPages; $page++) {
    try {
        $res   = Invoke-RestMethod -Method Get -Uri "$Base/movies?page=$page&limit=50" -Headers $headers -TimeoutSec 60
        $items = @($res.items)
    } catch {
        Write-Host "page ${page}: list failed - $($_.Exception.Message)" -ForegroundColor Red
        $failed++
        continue
    }

    if ($items.Count -eq 0) { Write-Host "page ${page}: no rows, done."; break }

    $ids = @($items | ForEach-Object { $_.id } | Where-Object { $_ -gt 0 })

    for ($i = 0; $i -lt $ids.Count; $i += $Batch) {
        $last  = [Math]::Min($i + $Batch - 1, $ids.Count - 1)
        $chunk = @($ids[$i..$last])
        $body  = @{ ids = $chunk } | ConvertTo-Json -Compress

        try {
            $r      = Invoke-RestMethod -Method Post -Uri "$Base/admin/enrich" `
                        -Headers $headers -ContentType "application/json" `
                        -Body $body -TimeoutSec 180
            $total += [int]$r.updated
            Write-Host ("page {0}: enriched {1}  (running total {2})" -f $page, $r.updated, $total)
        } catch {
            $failed++
            Write-Host ("page {0}: batch failed - {1}" -f $page, $_.Exception.Message) -ForegroundColor Red
        }

        Start-Sleep -Milliseconds 400   # be polite to TMDB / OMDb
    }
}

Write-Host ""
Write-Host "Done. Enriched $total rows, $failed failed batches." -ForegroundColor Green
Write-Host "Spot-check one movie:"
try {
    $one = Invoke-RestMethod -Method Get -Uri "$Base/movies/1" -Headers $headers -TimeoutSec 60
    $m   = $one.movie
    Write-Host ("  id 1 -> rt_rating={0}  metacritic={1}  budget={2}  revenue={3}  trailer_key={4}" -f `
        $m.rt_rating, $m.metacritic, $m.budget, $m.revenue, $m.trailer_key)
} catch {
    Write-Host "  (check skipped: $($_.Exception.Message))" -ForegroundColor Yellow
}
