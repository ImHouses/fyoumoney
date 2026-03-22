#!/usr/bin/env bash
set -euo pipefail

ktlint -F "src/**/*.kt" "src/**/*.kts"
