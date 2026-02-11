#!/bin/bash
# filepath: /home/andriantsoa/IdeaProjects/programmation_sys/backend/tools/start_all.sh
# Start all: primaries, load balancer, and test clients

set -euo pipefail
script_dir="$(cd "$(dirname "$0")" && pwd)"

echo "Starting primaries..."
"$script_dir/start_primaries.sh"

sleep 2

echo "Starting load balancer..."
cd "$script_dir/../../loadbalancer"
if [ ! -d "out" ]; then
    javac -cp "lib/json-simple-1.1.1.jar" -d out src/*.java
fi
java -cp "out:lib/json-simple-1.1.1.jar" LoadBalancer &
echo $! > "$script_dir/lb.pid"

sleep 2

echo "Starting test clients..."
cd "$script_dir/../../client"
if [ ! -d "out" ]; then
    javac -cp "lib/json-simple-1.1.1.jar" -d out src/MainClient.java src/Service/ServeurService.java src/model/Serveur.java src/model/User.java
fi

# Start multiple clients in background
java -cp "out:lib/json-simple-1.1.1.jar" MainClient &
echo $! > "$script_dir/client1.pid"

echo "All started. Use kill to stop."
