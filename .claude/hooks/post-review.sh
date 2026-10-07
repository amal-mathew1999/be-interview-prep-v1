#!/bin/bash
# Post-Review Hook
# Triggered after the reviewer agent completes a review.
# If no blockers found, automatically proceeds to PR creation.

set -euo pipefail

REVIEW_RESULT="${1:-UNKNOWN}"
BRANCH=$(git branch --show-current)

echo "=== Post-Review Hook ==="
echo "Review result: $REVIEW_RESULT"
echo "Branch: $BRANCH"

if [ "$REVIEW_RESULT" = "APPROVE" ]; then
    echo "Review passed. Proceeding to PR creation..."
    # Trigger post-task hook to create PR
    bash "$(dirname "$0")/post-task.sh"
elif [ "$REVIEW_RESULT" = "REQUEST_CHANGES" ]; then
    echo "Review found issues. Fix them before creating a PR."
    echo "Run '/review' again after fixing."
    exit 1
else
    echo "Unknown review result: $REVIEW_RESULT"
    exit 1
fi

echo "=== Post-Review Hook Complete ==="
