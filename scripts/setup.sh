#!/usr/bin/env sh
# © 2026 Ayoub — Tous droits réservés
set -eu

printf 'Vigilex setup - Linux/macOS\n'
command -v java >/dev/null || { echo 'Install JDK 21 (Temurin).'; exit 1; }
command -v node >/dev/null || { echo 'Install Node.js 20+.'; exit 1; }
command -v docker >/dev/null || { echo 'Install Docker and Docker Compose.'; exit 1; }

cd "$(dirname "$0")/.."
cat > docs/SETUP_REPORT.md <<EOF
# Vigilex setup report

Date: $(date -Iseconds)

- Java: $(java -version 2>&1 | head -n 1)
- Node: $(node --version)
- npm: $(npm --version)
- Docker: $(docker --version)
EOF
