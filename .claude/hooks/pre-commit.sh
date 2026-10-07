#!/bin/bash
# Pre-Commit Hook
# Runs before each commit to enforce quality gates.

set -euo pipefail

echo "=== Pre-Commit Hook ==="

# 1. Check for secrets/credentials in staged files
echo "Checking for secrets..."
SECRETS_PATTERN='(password|secret|api[_-]?key|token|credential|private[_-]?key)\s*[:=]\s*["\x27][^"\x27]+'
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)

for file in $STAGED_FILES; do
    if grep -iEn "$SECRETS_PATTERN" "$file" 2>/dev/null; then
        echo "ERROR: Potential secret found in $file"
        echo "Remove the secret before committing."
        exit 1
    fi
done

# 2. Check for forbidden patterns in Java files
echo "Checking Java conventions..."
JAVA_FILES=$(echo "$STAGED_FILES" | grep '\.java$' || true)

for file in $JAVA_FILES; do
    # No wildcard imports
    if grep -n 'import .*\.\*;' "$file" 2>/dev/null; then
        echo "ERROR: Wildcard import found in $file"
        echo "Use explicit imports instead."
        exit 1
    fi

    # No System.out.println (use logger instead)
    if grep -n 'System\.out\.print' "$file" 2>/dev/null; then
        echo "WARNING: System.out.print found in $file — use a logger instead."
    fi

    # No TODO without ticket reference
    if grep -n 'TODO[^(]' "$file" 2>/dev/null | grep -v 'TODO([A-Z]*-[0-9])' 2>/dev/null; then
        echo "WARNING: TODO without ticket reference in $file — use TODO(TICKET-123) format."
    fi
done

# 3. Check for large files
for file in $STAGED_FILES; do
    FILE_SIZE=$(wc -c < "$file" 2>/dev/null || echo "0")
    if [ "$FILE_SIZE" -gt 1000000 ]; then
        echo "ERROR: File $file is over 1MB ($FILE_SIZE bytes). Add to .gitignore or use Git LFS."
        exit 1
    fi
done

echo "=== Pre-Commit Hook Passed ==="
