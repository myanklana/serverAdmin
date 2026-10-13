# Creates .env with a random database password (only if it does not exist yet).
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
if (Test-Path $envFile) { Write-Host '.env ja existe; nada a fazer.'; return }
$bytes = New-Object byte[] 24
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
$password = ([BitConverter]::ToString($bytes) -replace '-', '').ToLower()
(Get-Content (Join-Path $root '.env.example')) -replace '^POSTGRES_PASSWORD=.*', "POSTGRES_PASSWORD=$password" |
    Set-Content -Path $envFile -Encoding ascii
Write-Host '.env criado. Inicie com: docker compose up -d --build'
