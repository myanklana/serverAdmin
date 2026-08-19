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
$configJson = @{ server = $ApiUrl.TrimEnd('/'); token = $Token } | ConvertTo-Json
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($configPath, $configJson, $utf8WithoutBom)
Write-Host "Enviando metricas para $($ApiUrl.TrimEnd('/')) a cada 5 segundos. Use Ctrl+C para parar." -ForegroundColor Green
& java -jar $JarPath $configPath
