#!/bin/bash
# Stop primary MainServer instances started by start_primaries.sh
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"

stop_pid_file() {
  local pid_file="$1"
  if [ -f "$pid_file" ]; then
    local pid
    pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      echo "Stopping PID $pid ($pid_file)"
      kill "$pid" 2>/dev/null || true
      sleep 0.5
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi
}

stop_pid_file "$script_dir/primary1.pid"
stop_pid_file "$script_dir/primary2.pid"
stop_pid_file "$script_dir/primary3.pid"

echo "Primaries stopped (if they were running)."
