#!/bin/bash
# Start SlaveServer (replication receiver) in background
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
cd "$repo_root"

# Kill existing SlaveServer processes
pkill -f SlaveServer || true
sleep 1

echo "Compiling server (includes SlaveServer)..."
mkdir -p out
javac -cp "lib/json-simple-1.1.1.jar" -d out src/*.java

echo "Starting slave server (port from server/resources/server_config.json)..."
java -cp "out:lib/json-simple-1.1.1.jar" SlaveServer &
echo $! > "$script_dir/slave.pid"

echo "Slave started. PID saved in $script_dir/slave.pid";
