#!/usr/bin/env bash
set -euo pipefail

# SmartDrive helper to run Docker Compose reliably.
# - Prefer the Compose v2 plugin if available: `docker compose ...`
# - Otherwise, run Compose v2 via the official docker/compose image.

if docker compose version >/dev/null 2>&1; then
  exec docker compose "$@"
fi

exec docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$PWD:$PWD" \
  -w "$PWD" \
  docker/compose:latest "$@"
