[CmdletBinding()]
param(
    [string]$JwtSecret,
    [string]$ApiUrl = 'http://localhost:8080',
    [string]$FrontendUrl = 'http://localhost:5173'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "O comando '$Name' nao foi encontrado. Instale-o e tente novamente."
    }
}

Require-Command docker
Require-Command node
Require-Command npm

if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
    $randomBytes = New-Object byte[] 48
    $randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $randomGenerator.GetBytes($randomBytes) }
    finally { $randomGenerator.Dispose() }
    $JwtSecret = [Convert]::ToBase64String($randomBytes)
    Write-Host 'APP_JWT_SECRET foi gerado apenas para esta execucao.' -ForegroundColor Yellow
}

Push-Location $root
try {
    docker compose up -d

    if (-not (Test-Path (Join-Path $root 'frontend/node_modules'))) {
        Push-Location (Join-Path $root 'frontend')
        try { npm.cmd ci } finally { Pop-Location }
    }

    $apiCommand = "`$env:APP_JWT_SECRET='$JwtSecret'; `$env:APP_CORS_ALLOWED_ORIGINS='$FrontendUrl'; Set-Location '$root/api'; .\mvnw.cmd spring-boot:run"
    Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $apiCommand

    $frontendCommand = "`$env:VITE_API_URL='$ApiUrl'; Set-Location '$root/frontend'; npm.cmd run dev -- --host 0.0.0.0"
    Start-Process powershell -ArgumentList '-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $frontendCommand

    Write-Host "API: $ApiUrl" -ForegroundColor Green
    Write-Host "Painel: $FrontendUrl" -ForegroundColor Green
    Write-Host 'Aguarde a API terminar de iniciar antes de abrir o painel.'
}
finally {
    Pop-Location
}
