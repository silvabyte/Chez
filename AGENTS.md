# BoogieLoops Project - Agent Instructions

## Build & Test Commands

- **Compile all:** `./mill __.compile`
- **Test all:** `./mill __.test`
- **Test single module:** `./mill schema.test` or `./mill web.test` or `./mill ai.test`
- **Run single test:** `./mill schema.test boogieloops.schema.primitives.StringSchemaTests`
- **Lint/Format:** `./mill __.fix` (scalafix) or `make format` (scalafmt)
- **Clean:** `./mill clean` or `make clean`

## Code Style

- **Language:** Scala 3.6.2+ with Mill build tool
- **Packages:** lowercase (e.g., `boogieloops.schema`, `boogieloops.web`, `boogieloops.ai`)
- **Classes:** PascalCase ending in `Schema` for schema types (e.g., `StringSchema`, `ObjectSchema`)
- **Imports:** Group by java/scala/third-party/local, merge same package, use `_root_` prefix when needed
- **Formatting:** Max 120 chars/line, 2-space indent, Asterisk-style docs, no vertical alignment
- **Testing:** utest framework, tests in parallel `test/src` structure mirroring main
- **Error handling:** Return `ValidationResult` with errors, avoid exceptions
- **No comments:** Unless explicitly requested, avoid adding code comments
- **Dependencies:** Check existing before adding - upickle for JSON, os-lib for file ops, cask for web

## Beads Workflow (Issue Tracking)

This project uses [beads](https://github.com/steveyegge/beads) for issue tracking. Run `bd prime` after context compaction or new sessions.

### Session Close Protocol

Before ending a session, ALWAYS run:

```bash
git status              # check what changed
git add <files>         # stage code changes
bd sync                 # commit beads changes
git commit -m "..."     # commit code
git push                # push to remote
```

### Essential Commands

- `bd ready` - Show issues ready to work (no blockers)
- `bd list --status=open` - All open issues
- `bd create --title="..." --type=task|bug|feature` - New issue
- `bd update <id> --status=in_progress` - Claim work
- `bd close <id> --reason="..."` - Mark complete
- `bd dep add <issue> <depends-on>` - Add dependency
- `bd sync` - Sync with git remote
