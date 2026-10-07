#!/bin/bash
# Create a new task branch from main
# Usage: ./scripts/new-task.sh <ticket-id> <description>
# Example: ./scripts/new-task.sh 42 add-user-authentication

set -euo pipefail

TICKET_ID="${1:?Usage: new-task.sh <ticket-id> <description>}"
DESCRIPTION="${2:?Usage: new-task.sh <ticket-id> <description>}"
BRANCH_NAME="task/${TICKET_ID}-${DESCRIPTION}"

echo "=== Creating New Task ==="
echo "Ticket: $TICKET_ID"
echo "Description: $DESCRIPTION"
echo "Branch: $BRANCH_NAME"

# Ensure clean working tree
if [ -n "$(git status --porcelain)" ]; then
    echo "ERROR: Working tree is not clean. Commit or stash changes first."
    git status --short
    exit 1
fi

# Update main and create branch
git checkout main
git pull origin main
git checkout -b "$BRANCH_NAME"

echo ""
echo "=== Task Ready ==="
echo "You are now on branch: $BRANCH_NAME"
echo "Make your changes, then run: ./scripts/complete-task.sh"
