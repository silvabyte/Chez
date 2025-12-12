##@ SDK

.PHONY: sdk-install
sdk-install: ## Install TypeScript SDK dependencies
	@cd typescript-sdk && npm ci

.PHONY: sdk-clean
sdk-clean: ## Remove generated SDK client
	@rm -rf typescript-sdk/src/client

.PHONY: sdk-generate
sdk-generate: ## Generate TS client from running API (requires example-web-api)
	@cd typescript-sdk && npm run openapi-ts

.PHONY: sdk-refresh
sdk-refresh: sdk-clean sdk-generate ## Clean and regenerate the TS client

.PHONY: sdk-demo
sdk-demo: ## Run a small TS demo against the API using the generated client
	@cd typescript-sdk && npm run demo

