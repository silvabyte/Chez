# Chez Project - Agent Instructions

## Build & Test Commands
- **Compile all:** `./mill __.compile`
- **Test all:** `./mill __.test`
- **Test single module:** `./mill Chez.test` or `./mill CaskChez.test` or `./mill ChezWiz.test`
- **Run single test:** `./mill Chez.test chez.primitives.StringChezTests`
- **Lint/Format:** `./mill __.fix` (scalafix) or `make format` (scalafmt)
- **Clean:** `./mill clean` or `make clean`

## Code Style
- **Language:** Scala 3.6.2+ with Mill build tool
- **Packages:** lowercase (e.g., `chez`, `caskchez`, `chezwiz`)
- **Classes:** PascalCase ending in `Chez` for schema types (e.g., `StringChez`, `ObjectChez`)
- **Imports:** Group by java/scala/third-party/local, merge same package, use `_root_` prefix when needed
- **Formatting:** Max 120 chars/line, 2-space indent, Asterisk-style docs, no vertical alignment
- **Testing:** utest framework, tests in parallel `test/src` structure mirroring main
- **Error handling:** Return `ValidationResult` with errors, avoid exceptions
- **No comments:** Unless explicitly requested, avoid adding code comments
- **Dependencies:** Check existing before adding - upickle for JSON, os-lib for file ops, cask for web