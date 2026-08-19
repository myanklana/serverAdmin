[CmdletBinding()]
param(
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$wrapper = Join-Path $root 'api/mvnw.cmd'
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) { $OutputDirectory = Join-Path $root 'dist' }

if (-not (Test-Path $wrapper)) { throw 'Maven Wrapper da API nao foi encontrado.' }

Push-Location (Join-Path $root 'api')
try {
    & .\mvnw.cmd -f ..\agent\pom.xml package
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel compilar o agente.' }
}
finally {
    Pop-Location
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$jar = Join-Path $root 'agent/target/agent-0.0.1-SNAPSHOT.jar'
$destination = Join-Path $OutputDirectory 'server-manager-agent.jar'
Copy-Item -Force $jar $destination
Write-Host "Agente pronto para copiar ao servidor: $destination" -ForegroundColor Green
