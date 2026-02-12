#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK_NAME="smartdrive_net"

SERVER_IMAGE="smartdrive_server"
LB_IMAGE="smartdrive_loadbalancer"
WEB_IMAGE="smartdrive_web"

c_rm() {
  local name="$1"
  docker rm -f "$name" >/dev/null 2>&1 || true
}

echo "[1/4] Build images"
docker build -t "$SERVER_IMAGE" "$ROOT_DIR/backend/server"
docker build -t "$LB_IMAGE" "$ROOT_DIR/backend/loadbalancer"
docker build -t "$WEB_IMAGE" "$ROOT_DIR/backend/client"

echo "[2/4] Create network (if needed)"
docker network inspect "$NETWORK_NAME" >/dev/null 2>&1 || docker network create "$NETWORK_NAME" >/dev/null

echo "[3/4] Remove old containers (if any)"
c_rm smartdrive_server1
c_rm smartdrive_server2
c_rm smartdrive_server3
c_rm smartdrive_loadbalancer
c_rm smartdrive_web

echo "[4/4] Start containers"
# Primary servers (only inside Docker network)
docker run -d \
  --name smartdrive_server1 \
  --network "$NETWORK_NAME" \
  --network-alias server1 \
  -v "$ROOT_DIR/backend/server/resources:/app/resources" \
  -v "$ROOT_DIR/backend/server/shared_storage:/app/shared_storage" \
  "$SERVER_IMAGE" 2121 >/dev/null

docker run -d \
  --name smartdrive_server2 \
  --network "$NETWORK_NAME" \
  --network-alias server2 \
  -v "$ROOT_DIR/backend/server/resources:/app/resources" \
  -v "$ROOT_DIR/backend/server/shared_storage:/app/shared_storage" \
  "$SERVER_IMAGE" 2122 >/dev/null

docker run -d \
  --name smartdrive_server3 \
  --network "$NETWORK_NAME" \
  --network-alias server3 \
  -v "$ROOT_DIR/backend/server/resources:/app/resources" \
  -v "$ROOT_DIR/backend/server/shared_storage:/app/shared_storage" \
  "$SERVER_IMAGE" 2123 >/dev/null

# Load balancer
docker run -d \
  --name smartdrive_loadbalancer \
  --network "$NETWORK_NAME" \
  --network-alias loadbalancer \
  -p 2100:2100 \
  -v "$ROOT_DIR/backend/loadbalancer/resources:/app/resources:ro" \
  -e SMARTDRIVE_LB_CONFIG=/app/resources/lb_config.docker.json \
  "$LB_IMAGE" >/dev/null

# Web (Tomcat)
docker run -d \
  --name smartdrive_web \
  --network "$NETWORK_NAME" \
  -p 8080:8080 \
  -e SMARTDRIVE_BACKEND_HOST=loadbalancer \
  -e SMARTDRIVE_BACKEND_PORT=2100 \
  "$WEB_IMAGE" >/dev/null

echo "OK: http://localhost:8080/SmartDrive/"
