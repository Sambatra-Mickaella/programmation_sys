#!/bin/bash
# Stop primaries + load balancer + slave
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"

"$script_dir/stop_loadbalancer.sh" || true
"$script_dir/stop_primaries.sh" || true
"$script_dir/stop_slave.sh" || true

echo "All SmartDrive services stopped."
