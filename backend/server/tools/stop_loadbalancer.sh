#!/bin/bash
# Stop LoadBalancer started by start_loadbalancer.sh
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
pid_file="$script_dir/lb.pid"

if [ -f "$pid_file" ]; then
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
    echo "Stopping LoadBalancer PID $pid"
    kill "$pid" 2>/dev/null || true
    sleep 0.5
    kill -9 "$pid" 2>/dev/null || true
  fi
  rm -f "$pid_file"
fi

echo "Load balancer stopped (if it was running)."
