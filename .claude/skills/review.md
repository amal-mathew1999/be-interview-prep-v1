# Skill: /review

Trigger: User invokes `/review`

## Description
Runs a comprehensive code review on all changes in the current branch compared to the base branch.

## Steps

1. Determine the base branch (default: `main`)
2. Get the diff: `git diff main...HEAD`
3. Delegate to the **Reviewer Agent** (`.claude/agents/reviewer.md`)
4. Present the review report to the user
5. If BLOCKERs are found:
   - List them clearly
   - Ask the user if they want auto-fixes applied
   - If yes, fix each blocker, re-run tests, and re-review
6. If no BLOCKERs:
   - Show the full report
   - Confirm the code is ready for PR
