#!/usr/bin/env bash
# Copies the kit's source files into your app module, swapping the placeholder package for yours.
#   ./install.sh <your.package.name> <path to your app module> [--force]
# Existing files are skipped unless --force is given (so merge your own AndroidManifest.xml by hand).
# This copies files only. The Gradle, strings and wiring steps are in README.md.
set -euo pipefail

if [ $# -lt 2 ]; then
  echo "usage: $0 <your.package.name> <path to your app module> [--force]" >&2
  exit 1
fi
PKG="$1"
FORCE="${3:-}"
KIT="$(cd "$(dirname "$0")" && pwd)"
PKG_PATH="$(echo "$PKG" | tr . /)"

# Made absolute now: the script changes directory below, which would break a relative path.
[ -d "$2" ] || { echo "no such app module folder: $2" >&2; exit 1; }
APP="$(cd "$2" && pwd)"

cd "$KIT/files"
find . -type f | sort | while read -r file; do
  rel="${file#./}"
  dest="$APP/src/$(echo "$rel" | sed "s#PACKAGE_PATH#$PKG_PATH#")"
  if [ -e "$dest" ] && [ "$FORCE" != "--force" ]; then
    echo "SKIP (exists): $dest"
    continue
  fi
  mkdir -p "$(dirname "$dest")"
  perl -pe "s/__PACKAGE__/$PKG/g" "$file" > "$dest"
  echo "wrote $dest"
done
echo "Done. Now follow README.md: Gradle, strings, backup rules and the wiring."
