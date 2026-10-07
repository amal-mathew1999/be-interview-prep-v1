#!/bin/bash
# Complete the current task: run tests, push, and create PR
# Usage: ./scripts/complete-task.sh

set -euo pipefail

BRANCH=$(git branch --show-current)
BASE_BRANCH="${BASE_BRANCH:-main}"

echo "=== Completing Task ==="
echo "Branch: $BRANCH"

# Guard: not on main
if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ]; then
    echo "ERROR: You're on $BRANCH. Switch to a task branch first."
    exit 1
fi

# Guard: has commits
COMMITS_AHEAD=$(git rev-list --count "$BASE_BRANCH".."$BRANCH" 2>/dev/null || echo "0")
if [ "$COMMITS_AHEAD" -eq 0 ]; then
    echo "ERROR: No commits on this branch. Nothing to complete."
    exit 1
fi

# Check for uncommitted changes
if [ -n "$(git status --porcelain)" ]; then
    echo "WARNING: You have uncommitted changes:"
    git status --short
    echo ""
    read -rp "Commit them now? (y/n) " COMMIT_CHOICE
    if [ "$COMMIT_CHOICE" = "y" ]; then
        read -rp "Commit message: " COMMIT_MSG
        git add -A
        git commit -m "$COMMIT_MSG"
    else
        echo "Aborting. Commit or stash your changes first."
        exit 1
    fi
fi

# Run tests if build tool is available
if [ -f "pom.xml" ]; then
    echo "Running Maven tests..."
    mvn test -q || { echo "ERROR: Tests failed. Fix before completing task."; exit 1; }
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    echo "Running Gradle tests..."
    ./gradlew test -q || { echo "ERROR: Tests failed. Fix before completing task."; exit 1; }
else
    echo "No build tool detected. Skipping tests."
fi

# Trigger the post-task hook (push + PR)
echo ""
echo "Running post-task hook..."
bash .claude/hooks/post-task.sh

echo ""
echo "=== Task Complete ==="
