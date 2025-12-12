##@ Test

.PHONY: test t
test: ## Run tests (override MODULE=Schema|Web|AI, SUITE=<Class/regex>)
	@if [ -n "$(strip $(SUITE))" ]; then \
	  echo "Running $(MODULE).test $(SUITE)"; \
	  $(MILL) $(MODULE).test $(SUITE); \
	else \
	  $(MILL) $(MODULE).test; \
	fi
t: test ## Alias for test

# Pattern: make test-Schema or test-Web or test-AI
.PHONY: test-%
test-%: ## Run tests for a specific module (e.g., test-Schema)
	@$(MILL) $*.test

# Convenience aliases
.PHONY: ts tw ta
ts: ## Run Schema tests
	@$(MILL) Schema.test
tw: ## Run Web tests
	@$(MILL) Web.test
ta: ## Run AI tests
	@$(MILL) AI.test

# README-friendly aliases
.PHONY: test-schema test-web test-ai
test-schema: ## Alias: Run Schema tests
	@$(MILL) Schema.test
test-web: ## Alias: Run Web tests
	@$(MILL) Web.test
test-ai: ## Alias: Run AI tests
	@$(MILL) AI.test

# Specific test suite groups
.PHONY: tp td tv ti
tp: ## Run Schema primitives tests
	@$(MILL) Schema.test boogieloops.schema.primitives
td: ## Run Schema derivation tests
	@$(MILL) Schema.test boogieloops.schema.derivation
tv: ## Run Schema validation tests
	@$(MILL) Schema.test boogieloops.schema.validation
ti: ## Run Web integration tests
	@$(MILL) Web.test boogieloops.web.UserCrudAPITest
