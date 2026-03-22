#!/usr/bin/env bash
set -euo pipefail

ktlint -F "src/**/*.kt" "src/**/*.kts"

if ! git diff --quiet -- 'src/**/*.kt' 'src/**/*.kts'; then
  git add -u -- 'src/'
  git commit -m "style: apply ktlint formatting"
fi
