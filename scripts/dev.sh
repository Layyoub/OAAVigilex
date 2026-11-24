#!/usr/bin/env sh
# © 2026 Ayoub — Tous droits réservés
set -eu
cd "$(dirname "$0")/.."
docker compose --project-directory infra -f infra/docker-compose.yml up -d --build
