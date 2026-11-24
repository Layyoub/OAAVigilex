<#
© 2026 Ayoub — Tous droits réservés
#>
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

Write-Host "Vigilex setup - Windows" -ForegroundColor Cyan

$javaCandidate = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.10002-hotspot\bin"
if (Test-Path $javaCandidate) { $env:JAVA_HOME = $javaCandidate.TrimEnd('\bin'); $env:PATH = "$javaCandidate;$env:PATH" }

function Ensure-Tool($Id, $Name) {
  $tool = Get-Command $Name -ErrorAction SilentlyContinue
  if (-not $tool) {
    Write-Host "Installing $Id..." -ForegroundColor Yellow
    winget install --exact --id $Id --silent --accept-package-agreements --accept-source-agreements
  }
}

Ensure-Tool "EclipseAdoptium.Temurin.21.JDK" "java"
Ensure-Tool "OpenJS.NodeJS.LTS" "node"
Ensure-Tool "Git.Git" "git"
Ensure-Tool "Docker.DockerDockerDesktop" "docker"

$versions = @(
  @{ Name = "Java"; Command = "java"; Args = "-version" },
  @{ Name = "Node"; Command = "node"; Args = "--version" },
  @{ Name = "npm"; Command = "npm"; Args = "--version" },
  @{ Name = "Git"; Command = "git"; Args = "--version" },
  @{ Name = "Docker"; Command = "docker"; Args = "--version" }
)

$report = @("# Vigilex setup report", "", "Date: $(Get-Date -Format o)", "")
foreach ($item in $versions) {
  $exists = Get-Command $item.Command -ErrorAction SilentlyContinue
  if ($exists) {
    $value = & $item.Command $item.Args 2>&1 | Select-Object -First 1
    $line = "- $($item.Name): $value"
  } else {
    $line = "- $($item.Name): not found"
  }
  $report += $line
  Write-Host $line
}

Set-Location (Split-Path -Parent $PSScriptRoot)
$report | Set-Content -Encoding UTF8 "docs\SETUP_REPORT.md"
Write-Host "Setup complete." -ForegroundColor Green
