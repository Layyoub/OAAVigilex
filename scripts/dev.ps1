<#
© 2026 Ayoub — Tous droits réservés
#>
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

docker compose --project-directory infra -f infra\docker-compose.yml up -d --build
Write-Host "Vigilex is starting:"
Write-Host "  Frontend: http://localhost"
Write-Host "  Backend:  http://localhost:8080"
Write-Host "  Mailpit:  http://localhost:8025"
