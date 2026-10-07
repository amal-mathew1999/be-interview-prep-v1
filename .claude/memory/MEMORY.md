# Project Memory

## Project Type
- Java project with Claude Code harness layer
- Atomic task workflow: each task = one branch = one PR

## Key Paths
- Agents: `.claude/agents/` (reviewer.md, task.md, pr.md)
- Skills: `.claude/skills/` (review.md, task.md, pr.md)
- Hooks: `.claude/hooks/` (post-task.sh, pre-commit.sh, post-review.sh)
- Scripts: `scripts/` (setup-hooks.sh, new-task.sh, complete-task.sh)
- Settings: `.claude/settings.json`

## Workflow
1. `/task <description>` — starts atomic task on new branch
2. Implement + test on task branch
3. `/review` — runs reviewer agent
4. `/pr` or auto via post-task hook — creates PR
5. PR gets reviewed and merged to main
