# Skill: /pr

Trigger: User invokes `/pr`

## Description
Creates a pull request from the current branch. Runs a review first, then delegates to the PR Agent.

## Steps

1. **Pre-flight checks**:
   - Verify not on `main` branch
   - Verify there are commits ahead of `main`
   - Verify working tree is clean (no uncommitted changes)

2. **Run review**: Invoke the Reviewer Agent
   - If BLOCKERs found, report them and abort PR creation
   - Offer to fix BLOCKERs automatically

3. **Push branch**: `git push -u origin <current-branch>`

4. **Create PR**: Delegate to PR Agent (`.claude/agents/pr.md`)
   - Generate title and body from commits and diff
   - Apply appropriate labels

5. **Report**: Show the PR URL to the user
