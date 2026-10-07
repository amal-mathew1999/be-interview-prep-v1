# Java Project — Claude Code Harness

## Project Overview
This is a Java project managed with Claude Code agents, skills, hooks, and conventions.
All work is done via **atomic tasks** — each task is a self-contained unit that results in a PR.

## Conventions

### Code Style
- Follow Google Java Style Guide
- Use 4-space indentation (no tabs)
- Max line length: 120 characters
- All public methods must have Javadoc
- Use `final` for variables that don't change
- Prefer composition over inheritance
- No wildcard imports (`import java.util.*` is banned)
- Use meaningful variable names — no single-letter names except loop counters

### Git Workflow
- Branch naming: `task/<ticket-id>-<short-description>` (e.g., `task/42-add-user-auth`)
- One atomic task = one branch = one PR
- Commit messages: imperative mood, max 72 chars subject line
- Every commit must pass all tests before being pushed
- Never push directly to `main` — always go through a PR

### Testing
- Every feature/bugfix must include corresponding tests
- Test class naming: `<ClassName>Test.java`
- Use JUnit 5 with AssertJ assertions
- Minimum 80% code coverage for new code
- Tests must be independent and idempotent

### Project Structure
```
src/
  main/
    java/          # Production source code
    resources/     # Config files, templates
  test/
    java/          # Test source code
    resources/     # Test fixtures
```

## Agents

### Reviewer Agent
- Defined in: `.claude/agents/reviewer.md`
- Purpose: Reviews all code changes for quality, security, and convention compliance
- Invoked automatically before PR creation

### Task Agent
- Defined in: `.claude/agents/task.md`
- Purpose: Executes atomic tasks — each task is a self-contained unit of work
- Creates a branch, implements changes, runs tests, opens a PR

### PR Agent
- Defined in: `.claude/agents/pr.md`
- Purpose: Handles PR creation, description formatting, and labeling

## Skills
- `/review` — Run the reviewer agent on current changes
- `/task` — Start a new atomic task
- `/pr` — Create a PR from the current branch

## Hooks
- **post-task**: Automatically triggers PR creation when a task is marked complete
- **pre-commit**: Runs linting and tests before allowing commits

## Memory
- See `.claude/memory/MEMORY.md` for persistent project knowledge
