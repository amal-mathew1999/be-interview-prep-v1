# Skill: /task

Trigger: User invokes `/task <description>`

## Description
Starts a new atomic task. Each task is a self-contained unit of work that results in exactly one PR.

## Parameters
- `description` (required): What the task should accomplish
- `ticket-id` (optional): Ticket/issue ID for branch naming

## Steps

1. **Parse the task**: Extract the description and optional ticket ID
2. **Assess scope**: If the task seems too large (likely > 400 lines), propose a breakdown into sub-tasks and confirm with the user
3. **Setup branch**:
   ```
   git checkout main
   git pull origin main
   git checkout -b task/<ticket-id>-<short-description>
   ```
4. **Delegate to Task Agent** (`.claude/agents/task.md`) for execution
5. **Implement**: Make changes following project conventions
6. **Test**: Run `mvn test` or `gradle test`
7. **Self-review**: Run the Reviewer Agent on changes
8. **Fix issues**: Address any BLOCKERs found in review
9. **Commit & Push**:
   ```
   git add <specific-files>
   git commit -m "<descriptive message>"
   git push -u origin <branch-name>
   ```
10. **Create PR**: Delegate to PR Agent to open the pull request
11. **Report**: Show the PR URL to the user

## Atomicity Guarantee
- If any step fails after branch creation, changes stay on the task branch (never pollute main)
- The user can resume or abandon the task
- Follow-up work discovered during the task is logged, not acted on
