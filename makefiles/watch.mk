##@ Watch

.PHONY: watch w
watch: ## Watch compile (override MODULE=...)
	@echo "👀 Watching for changes... (Ctrl+C to stop)"
	@$(MILL) -w $(MODULE).compile
w: watch ## Alias for watch

.PHONY: watch-test wt
watch-test: ## Watch tests (override MODULE=...)
	@echo "👀 Running tests on change... (Ctrl+C to stop)"
	@$(MILL) -w $(MODULE).test
wt: watch-test ## Alias for watch-test

