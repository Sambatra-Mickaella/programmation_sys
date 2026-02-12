#!/bin/bash
# Start LoadBalancer (TCP) in background
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

# Stop existing LB if running
if [ -f "$script_dir/lb.pid" ]; then
	old_pid="$(cat "$script_dir/lb.pid" 2>/dev/null || true)"
	if [ -n "$old_pid" ] && kill -0 "$old_pid" 2>/dev/null; then
		echo "Stopping existing LoadBalancer PID $old_pid..."
		kill "$old_pid" 2>/dev/null || true
		sleep 0.5
		kill -9 "$old_pid" 2>/dev/null || true
	fi
	rm -f "$script_dir/lb.pid"
fi

pkill -f "java.*LoadBalancer" 2>/dev/null || true
sleep 0.5

cd "$repo_root/../loadbalancer"

echo "Compiling load balancer..."
mkdir -p out
javac -cp "lib/json-simple-1.1.1.jar" -d out src/*.java

echo "Starting load balancer on port 2100..."
java -cp "out:lib/json-simple-1.1.1.jar" LoadBalancer &
echo $! > "$script_dir/lb.pid"

echo "Load balancer started. PID saved in $script_dir/lb.pid";
