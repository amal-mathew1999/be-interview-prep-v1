#!/bin/bash
# Post-Task Hook
# Triggered when an atomic task is marked as complete.
# Automatically creates a PR for the completed task.

set -euo pipefail

BRANCH=$(git branch --show-current)
BASE_BRANCH="${BASE_BRANCH:-main}"

echo "=== Post-Task Hook ==="
echo "Branch: $BRANCH"
echo "Base: $BASE_BRANCH"

# Guard: don't run on main
if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ]; then
    echo "ERROR: Cannot create PR from $BRANCH. Aborting."
    exit 1
fi

# Guard: check for commits ahead of base
COMMITS_AHEAD=$(git rev-list --count "$BASE_BRANCH".."$BRANCH" 2>/dev/null || echo "0")
if [ "$COMMITS_AHEAD" -eq 0 ]; then
    echo "ERROR: No commits ahead of $BASE_BRANCH. Nothing to PR."
    exit 1
fi

# Guard: check for clean working tree
if [ -n "$(git status --porcelain)" ]; then
    echo "WARNING: Uncommitted changes detected. Commit or stash before creating PR."
    echo "Uncommitted files:"
    git status --short
    exit 1
fi

# Push branch
echo "Pushing branch to remote..."
git push -u origin "$BRANCH"

# Check if PR already exists
EXISTING_PR=$(gh pr list --head "$BRANCH" --json number --jq '.[0].number' 2>/dev/null || echo "")
if [ -n "$EXISTING_PR" ]; then
    echo "PR #$EXISTING_PR already exists for this branch."
    echo "URL: $(gh pr view "$EXISTING_PR" --json url --jq '.url')"
    exit 0
fi

# Generate PR title from branch name
# task/42-add-user-auth -> Add user auth
PR_TITLE=$(echo "$BRANCH" | sed 's|task/||' | sed 's|^[0-9]*-||' | tr '-' ' ' | sed 's/\b\(.\)/\u\1/')

# Generate PR body from commit log
COMMIT_LOG=$(git log "$BASE_BRANCH".."$BRANCH" --pretty=format:"- %s" --reverse)
CHANGED_FILES=$(git diff "$BASE_BRANCH"..."$BRANCH" --stat)

PR_BODY=$(cat <<EOF
## Summary
$COMMIT_LOG

## Changed Files
\`\`\`
$CHANGED_FILES
\`\`\`

## Testing
- [ ] Unit tests added/updated
- [ ] All existing tests pass

---
Generated with Claude Code (Post-Task Hook)
EOF
)

echo "Creating PR..."
PR_URL=$(gh pr create \
    --title "$PR_TITLE" \
    --body "$PR_BODY" \
    --base "$BASE_BRANCH" \
    --head "$BRANCH" 2>&1)

echo "PR created: $PR_URL"
echo "=== Post-Task Hook Complete ==="
