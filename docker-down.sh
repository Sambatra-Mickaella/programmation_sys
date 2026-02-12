#!/usr/bin/env bash
set -euo pipefail

NETWORK_NAME="smartdrive_net"

for name in smartdrive_loadbalancer smartdrive_server1 smartdrive_server2 smartdrive_server3; do
  docker rm -f "$name" >/dev/null 2>&1 || true
done

docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true

echo "Stopped SmartDrive containers."
