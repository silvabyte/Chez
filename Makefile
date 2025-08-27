# Chez Project Makefile
# Quick commands for development, testing, and project management

.DEFAULT_GOAL := help
SHELL := /bin/bash

# ============================================================================
# HELP
# ============================================================================
.PHONY: help
help:
	@echo "Chez Project Commands"
	@echo "===================="
	@echo ""
	@echo "Core:"
	@echo "  make build     - Compile all modules"
	@echo "  make test      - Run all tests"
	@echo "  make clean     - Clean build artifacts"
	@echo "  make format    - Format code with scalafmt"
	@echo "  make lint      - Run scalafix linting"
	@echo ""
	@echo "Testing:"
	@echo "  make t         - Quick test (alias for test)"
	@echo "  make tc        - Test Chez module"
	@echo "  make tca       - Test CaskChez module"
	@echo "  make tw        - Test ChezWiz module"
	@echo "  make watch     - Run tests in watch mode"
	@echo ""
	@echo "Examples:"
	@echo "  make demo      - Run all Chez examples"
	@echo "  make wiz       - Run all ChezWiz AI examples"
	@echo ""
	@echo "Development:"
	@echo "  make repl      - Start Scala REPL"
	@echo "  make check     - Full check (compile + test + lint)"
	@echo "  make release   - Prepare release (check + assembly)"
	@echo ""
	@echo "Publishing:"
	@echo "  make publish-local    - Publish to local Ivy repo"
	@echo "  make publish-m2       - Publish to local Maven repo"
	@echo "  make publish-central  - Publish to Maven Central"
	@echo "  make publish-dry-run  - Test publishing config"
	@echo ""
	@echo "Use 'make <target> -n' to see what commands will run"

# ============================================================================
# CORE COMMANDS
# ============================================================================
.PHONY: build b
build:
	@./mill __.compile
b: build

.PHONY: test t
test:
	@./mill __.test
t: test

.PHONY: clean
clean:
	@./mill clean
	@rm -rf out/

.PHONY: format fmt
format:
	@./mill mill.scalalib.scalafmt.ScalafmtModule/reformatAll
fmt: format

.PHONY: lint fix
lint:
	@./mill __.fix
fix: lint

# ============================================================================
# MODULE-SPECIFIC TESTING
# ============================================================================
.PHONY: tc test-chez
tc:
	@./mill Chez.test
test-chez: tc

.PHONY: tca test-cask
tca:
	@./mill CaskChez.test
test-cask: tca

.PHONY: tw test-wiz
tw:
	@./mill ChezWiz.test
test-wiz: tw

# Specific test suites
.PHONY: tp test-primitives
tp:
	@./mill Chez.test chez.primitives
test-primitives: tp

.PHONY: td test-derivation
td:
	@./mill Chez.test chez.derivation
test-derivation: td

.PHONY: tv test-validation
tv:
	@./mill Chez.test chez.validation
test-validation: tv

.PHONY: ti test-integration
ti:
	@./mill CaskChez.test caskchez.UserCrudAPITest
test-integration: ti

# ============================================================================
# WATCH MODE
# ============================================================================
.PHONY: watch w
watch:
	@echo "👀 Watching for changes... (Ctrl+C to stop)"
	@./mill -w __.compile
w: watch

.PHONY: watch-test wt
watch-test:
	@echo "👀 Running tests on change... (Ctrl+C to stop)"
	@./mill -w __.test
wt: watch-test

# ============================================================================
# EXAMPLES & DEMOS
# ============================================================================
.PHONY: demo examples
demo:
	@./mill Chez.test.runMain chez.examples.BasicUsage
	@./mill Chez.test.runMain chez.examples.ComplexTypes
	@./mill Chez.test.runMain chez.examples.Validation
examples: demo

.PHONY: wiz wiz-demo
wiz:
	@./mill ChezWiz.runMain chezwiz.agent.examples.Examples
wiz-demo: wiz

# Individual provider examples
.PHONY: wiz-openai
wiz-openai:
	@./mill ChezWiz.runMain chezwiz.agent.examples.OpenAIExample

.PHONY: wiz-anthropic
wiz-anthropic:
	@./mill ChezWiz.runMain chezwiz.agent.examples.AnthropicExample

.PHONY: wiz-local
wiz-local:
	@./mill ChezWiz.runMain chezwiz.agent.examples.OpenAICompatibleExample

# ============================================================================
# DEVELOPMENT TOOLS
# ============================================================================
.PHONY: repl console
repl:
	@./mill Chez.console
console: repl

.PHONY: assembly jar
assembly:
	@./mill __.assembly
jar: assembly

.PHONY: docs
docs:
	@./mill __.docJar

.PHONY: deps
deps:
	@./mill mill.scalalib.Dependency/showUpdates

# ============================================================================
# CI/CD COMMANDS
# ============================================================================
.PHONY: check
check: build test lint
	@echo "✅ All checks passed!"

.PHONY: ci
ci: clean check
	@echo "✅ CI pipeline complete!"

.PHONY: release
release: check assembly docs
	@echo "📦 Release artifacts ready!"

# ============================================================================
# PUBLISHING
# ============================================================================
.PHONY: publish-local
publish-local:
	@./mill __.publishLocal
	@echo "✅ Published to local Ivy repository (~/.ivy2/local)"

.PHONY: publish-m2
publish-m2:
	@./mill __.publishM2Local
	@echo "✅ Published to local Maven repository (~/.m2/repository)"

.PHONY: publish-central
publish-central:
	@echo "Publishing to Maven Central..."
	@echo "Make sure you have set:"
	@echo "  - MILL_SONATYPE_USERNAME"
	@echo "  - MILL_SONATYPE_PASSWORD" 
	@echo "  - MILL_PGP_SECRET_BASE64"
	@echo "  - MILL_PGP_PASSPHRASE"
	@./mill mill.javalib.SonatypeCentralPublishModule/
	@echo "✅ Published to Maven Central!"

.PHONY: publish-dry-run
publish-dry-run:
	@echo "Dry run - checking publishing configuration..."
	@./mill __.publishLocal --transitive=false
	@echo "✅ Dry run complete - check ~/.ivy2/local for artifacts"

# ============================================================================
# PROJECT INFO
# ============================================================================
.PHONY: info
info:
	@echo "Chez Project"
	@echo "============"
	@echo "Scala:     3.6.2+"
	@echo "Build:     Mill"
	@echo "Test:      utest"
	@echo "Modules:   Chez (core), CaskChez (web), ChezWiz (AI)"
	@echo ""
	@echo "Run 'make help' for available commands"

.PHONY: version
version:
	@./mill version

# ============================================================================
# UTILITIES
# ============================================================================
.PHONY: tree
tree:
	@tree -I 'out|.git|.metals|.bloop|.bsp|target|node_modules' -L 2

.PHONY: loc
loc:
	@find . -name "*.scala" -not -path "./out/*" | xargs wc -l | tail -1

# Clean everything including IDE files
.PHONY: clean-all purge
clean-all:
	@./mill clean
	@rm -rf out/ .bloop .bsp .metals target
	@echo "✅ Deep clean complete!"
purge: clean-all