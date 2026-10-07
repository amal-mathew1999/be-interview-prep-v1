#!/bin/bash
# Setup script: installs git hooks from .claude/hooks into .git/hooks

set -euo pipefail

HOOKS_SOURCE=".claude/hooks"
HOOKS_TARGET=".git/hooks"

echo "=== Setting up Git Hooks ==="

if [ ! -d ".git" ]; then
    echo "Not a git repository. Run 'git init' first."
    exit 1
fi

mkdir -p "$HOOKS_TARGET"

# Install pre-commit hook
if [ -f "$HOOKS_SOURCE/pre-commit.sh" ]; then
    cp "$HOOKS_SOURCE/pre-commit.sh" "$HOOKS_TARGET/pre-commit"
    chmod +x "$HOOKS_TARGET/pre-commit"
    echo "Installed: pre-commit hook"
fi

echo "=== Git Hooks Setup Complete ==="
echo ""
echo "Installed hooks:"
ls -la "$HOOKS_TARGET/" 2>/dev/null | grep -v '\.sample$' | tail -n +2
