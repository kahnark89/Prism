#!/usr/bin/env bash
# Replace the client pointer files with real symlinks to AGENTS.md.
# Run only on filesystems that support symlinks (Linux/macOS, or Windows with dev mode).
set -e
for f in CLAUDE.md GEMINI.md; do
  rm -f "$f"
  ln -s AGENTS.md "$f"
  echo "linked $f -> AGENTS.md"
done
echo "Done. On Windows without symlink support, keep the pointer files instead."
