#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <username> <password> <owner> <filename>" >&2
  echo "Checks the first-line response to DOWNLOAD_AS (expects FILE;<size> or ERROR ...)." >&2
}

if [[ $# -ne 4 ]]; then
  usage
  exit 2
fi

username="$1"
password="$2"
owner="$3"
filename="$4"

exec 3<>"/dev/tcp/127.0.0.1/2121"

printf 'LOGIN;%s;%s\r\n' "$username" "$password" >&3
IFS= read -r login_resp <&3 || true
echo "$login_resp"

printf 'DOWNLOAD_AS;%s;%s\r\n' "$owner" "$filename" >&3
IFS= read -r header <&3 || true
echo "$header"

exec 3<&-
exec 3>&-

