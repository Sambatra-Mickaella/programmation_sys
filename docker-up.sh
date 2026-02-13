#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK_NAME="smartdrive_net"

SERVER_IMAGE="smartdrive_server"
LB_IMAGE="smartdrive_loadbalancer"
JAVAFX_IMAGE="smartdrive_javafx"

c_rm() {
  local name="$1"
  docker rm -f "$name" >/dev/null 2>&1 || true
}

echo "[1/4] Build images"
docker build -t "$SERVER_IMAGE" "$ROOT_DIR/backend/server"
docker build -t "$LB_IMAGE" "$ROOT_DIR/backend/loadbalancer"
docker build -f "$ROOT_DIR/backend/client/Dockerfile.javafx" -t "$JAVAFX_IMAGE" "$ROOT_DIR/backend/client"

echo "[2/4] Create network (if needed)"
docker network inspect "$NETWORK_NAME" >/dev/null 2>&1 || docker network create "$NETWORK_NAME" >/dev/null

echo "[3/4] Remove old containers (if any)"
c_rm smartdrive_server1
c_rm smartdrive_server2
c_rm smartdrive_server3
c_rm smartdrive_loadbalancer
c_rm smartdrive_javafx

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

# JavaFX client (GUI)
if [[ -z "${DISPLAY:-}" ]]; then
  echo "DISPLAY n'est pas defini. JavaFX ne peut pas s'afficher."
  echo "Definis DISPLAY puis relance (ex: export DISPLAY=:0)"
  exit 1
fi

docker run -d \
  --name smartdrive_javafx \
  --network "$NETWORK_NAME" \
  -e DISPLAY="$DISPLAY" \
  -e GDK_BACKEND=x11 \
  -e LANG=C.UTF-8 \
  -e LC_ALL=C.UTF-8 \
  -e LANGUAGE=C.UTF-8 \
  -e SMARTDRIVE_BACKEND_HOST=loadbalancer \
  -e SMARTDRIVE_BACKEND_PORT=2100 \
  -v /tmp/.X11-unix:/tmp/.X11-unix \
  -v "${SMARTDRIVE_HOST_HOME:-${HOME:-/tmp}}:/host-home" \
  "$JAVAFX_IMAGE" >/dev/null

echo "OK: backend + JavaFX demarres."
echo "Si la fenetre ne s'affiche pas, execute: xhost +local:docker"
