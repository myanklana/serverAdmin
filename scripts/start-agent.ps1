[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string]$ApiUrl,
    [Parameter(Mandatory)] [ValidateLength(32, 512)] [string]$Token,
    [string]$JarPath
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($JarPath)) { $JarPath = Join-Path $PSScriptRoot 'server-manager-agent.jar' }

if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Java 21 ou superior e necessario.' }
if (-not (Test-Path $JarPath)) { throw "JAR do agente nao encontrado: $JarPath" }
if ($ApiUrl -notmatch '^https?://') { throw 'ApiUrl deve iniciar com http:// ou https://.' }

$configPath = Join-Path (Split-Path -Parent $JarPath) 'config.json'
@{ server = $ApiUrl.TrimEnd('/'); token = $Token } | ConvertTo-Json | Set-Content -Path $configPath -Encoding utf8NoBOM
Write-Host "Enviando metricas para $($ApiUrl.TrimEnd('/')) a cada 5 segundos. Use Ctrl+C para parar." -ForegroundColor Green
& java -jar $JarPath $configPath
