# CLAUDE.md

## Follow-up Tracking

When you identify something that should be addressed but is out of scope for the current task, create a GitHub issue for it. This includes:

- Commented-out code that should be cleaned up or re-enabled
- TODO/FIXME comments
- Test coverage gaps discovered during development
- Features or fixes deferred to keep the current PR focused
- Workarounds that should be replaced with proper solutions
- Potential improvements to existing features (UX, performance, error handling)
- Code that works but could be designed better
- Deprecation warnings or dependency updates worth investigating

Keep issues concise with enough context to act on later. If the follow-up was discovered while working on an existing issue, link the new issue back to the original (e.g., "Discovered while working on #N").

## Working on Issues

When picking up a GitHub issue, treat the issue as the source of truth for all investigation, decisions, and progress.

### Investigation and decisions
- Document findings directly in the issue as comments — what you explored, what you learned, what options exist
- When making a design decision (e.g., choosing between approaches), write the trade-offs and rationale in the issue before implementing
- If something unexpected comes up during implementation (new constraint, dependency issue, scope change), document it in the issue
- Link to relevant code, files, or external resources in your comments

### Plans
- If a plan is generated for the work, post the full plan markdown as a comment on the issue
- If the plan changes during implementation, post the updated plan as a new comment (don't edit the old one — preserve the history)
- Tag plan comments with a heading like `## Implementation Plan` or `## Updated Plan`

### Progress tracking
- Use task lists in the issue body or comments to track progress:
  ```markdown
  - [x] Investigated approaches
  - [x] Implemented core change
  - [ ] Added tests
  - [ ] Updated documentation
  ```
- Update task lists as work progresses so the issue reflects current state
- Add a comment when starting work, when blocked, and when submitting a PR
- Link PRs to issues using `Closes #N` or `Part of #N` in the PR description

### When closing
- Ensure the issue has a final comment summarizing what was done (or link to the merged PR)
- If an issue is closed without being fully resolved, explain why and create follow-up issues if needed
