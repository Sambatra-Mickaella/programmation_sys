#!/bin/bash
# filepath: /home/andriantsoa/IdeaProjects/programmation_sys/backend/tools/start_primaries.sh
# Launch 3 primary servers on ports 2121, 2122, 2123
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
pid_dir="$(cd "$(dirname "$0")" && pwd)"
cd "$repo_root"

# Kill existing MainServer processes
pkill -f MainServer || true
sleep 2

echo "Compiling server..."
mkdir -p out
javac -cp "lib/json-simple-1.1.1.jar" -d out src/*.java

echo "Starting primary server on port 2121..."
java -cp "out:lib/json-simple-1.1.1.jar" MainServer 2121 &
echo $! > "$pid_dir/primary1.pid"

echo "Starting primary server on port 2122..."
java -cp "out:lib/json-simple-1.1.1.jar" MainServer 2122 &
echo $! > "$pid_dir/primary2.pid"

echo "Starting primary server on port 2123..."
java -cp "out:lib/json-simple-1.1.1.jar" MainServer 2123 &
echo $! > "$pid_dir/primary3.pid"

echo "Primaries started. PIDs saved in *.pid files."
