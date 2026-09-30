#!/usr/bin/env bash
# Records VGS's delta against upstream Starlark as bin/<date>-differences-libstarlark-<sha>.patch.
#
# The patch is `git diff -R` of libstarlark/src/{main,test}/java/net after upstream's tree is copied
# over it the way bin/update-starlark.py does (rsync --delete, BUILD files excluded), so
# "--- b/" is upstream's file and "+++ a/" is ours. VGS-only files appear as new files. The
# testdata directory is left out. VGS's own sources (src/{main,test}/vgs, and src/{main,test}/bc
# for the bytecode VMs) are not upstream code and are not part of it.
#
# The patch is checked before it is written: applied to a clean copy of upstream, it must rebuild
# our tree exactly.
#
# usage: bin/regenerate-differences-patch.sh [-u UPSTREAM_DIR] [-r REF] [-o OUT]
#   -u  upstream Bazel checkout (default: .tmp/bazel); its HEAD is the base
#   -r  our side: a commit (e.g. HEAD) instead of the working tree
#   -o  output file (default: bin/<upstream commit date>-differences-libstarlark-<sha9>.patch)
set -euo pipefail

REPO=$(git -C "$(dirname "$0")/.." rev-parse --show-toplevel)
UPSTREAM=$REPO/.tmp/bazel
REF=
OUT=
while getopts "u:r:o:h" opt; do
  case $opt in
    u) UPSTREAM=$(cd "$OPTARG" && pwd) ;;
    r) REF=$OPTARG ;;
    o) OUT=$OPTARG ;;
    *) sed -n '2,20p' "$0"; exit 2 ;;
  esac
done

if ! git -C "$UPSTREAM" rev-parse --verify -q HEAD >/dev/null; then
  echo "error: $UPSTREAM is not a git checkout of bazelbuild/bazel" >&2
  exit 1
fi
if [ -n "$(git -C "$UPSTREAM" status --porcelain -- src/main/java/net src/test/java/net)" ]; then
  echo "error: $UPSTREAM has local changes under src/{main,test}/java/net" >&2
  exit 1
fi
BASE_SHA=$(git -C "$UPSTREAM" rev-parse --short=9 HEAD)
BASE_DATE=$(git -C "$UPSTREAM" log -1 --format=%cs HEAD)
OUT=${OUT:-$REPO/bin/$BASE_DATE-differences-libstarlark-$BASE_SHA.patch}

WORK=$(mktemp -d "${TMPDIR:-/tmp}/differences.XXXXXX")
trap 'rm -rf "$WORK"' EXIT
EXCLUDE=':!libstarlark/src/test/java/net/starlark/java/eval/testdata'

# Our tree, committed in a scratch repository.
OURS=$WORK/ours
mkdir -p "$OURS"
for d in main test; do
  mkdir -p "$OURS/libstarlark/src/$d/java"
  if [ -n "$REF" ]; then
    git -C "$REPO" archive "$REF" "libstarlark/src/$d/java/net" | tar -x -C "$OURS"
  else
    rsync -a "$REPO/libstarlark/src/$d/java/net/" "$OURS/libstarlark/src/$d/java/net/"
  fi
done
git -C "$OURS" init -q
git -C "$OURS" add -A
git -C "$OURS" -c user.name=x -c user.email=x@x -c commit.gpgsign=false commit -q -m ours

# Upstream's tree over it; the reverse diff is our delta.
for d in main test; do
  rsync -a --exclude=BUILD --delete \
    "$UPSTREAM/src/$d/java/net/" "$OURS/libstarlark/src/$d/java/net/"
done
git -C "$OURS" add -N --ignore-removal .
git -C "$OURS" diff -R --abbrev=8 -- libstarlark "$EXCLUDE" > "$WORK/patch"

# Check: upstream + patch == our tree.
CHECK=$WORK/check
for d in main test; do
  mkdir -p "$CHECK/libstarlark/src/$d/java"
  rsync -a --exclude=BUILD "$UPSTREAM/src/$d/java/net/" "$CHECK/libstarlark/src/$d/java/net/"
done
git -C "$CHECK" init -q
git -C "$CHECK" apply "$WORK/patch"
git -C "$OURS" checkout -q -- .
git -C "$OURS" clean -qfd
for d in main test; do
  diff -r --exclude=BUILD --exclude=testdata \
    "$CHECK/libstarlark/src/$d/java/net" "$OURS/libstarlark/src/$d/java/net" >/dev/null || {
    echo "error: the patch does not rebuild libstarlark/src/$d/java/net from upstream" >&2
    exit 1
  }
done

cp "$WORK/patch" "$OUT"
echo "$OUT: $(grep -c '^diff --git' "$OUT") files against upstream $BASE_SHA ($BASE_DATE)"
