##@ CI

.PHONY: check
check: build test lint ## Run build, tests, and linting
	@echo "✅ All checks passed!"

.PHONY: ci
ci: clean check ## Clean then run checks
	@echo "✅ CI pipeline complete!"

.PHONY: release
release: check assembly docs ## Prepare release artifacts
	@echo "📦 Release artifacts ready!"

