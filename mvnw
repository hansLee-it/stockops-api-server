#!/usr/bin/env bash
set -euo pipefail

if ! command -v mvn >/dev/null 2>&1; then
  printf '%s\n' 'Maven is required to run this wrapper.' >&2
  exit 1
fi

exec mvn "$@"
