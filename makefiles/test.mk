##@ Test

.PHONY: test t
test: ## Run tests (override MODULE=Chez|CaskChez|ChezWiz, SUITE=<Class/regex>)
	@if [ -n "$(strip $(SUITE))" ]; then \
	  echo "Running $(MODULE).test $(SUITE)"; \
	  $(MILL) $(MODULE).test $(SUITE); \
	else \
	  $(MILL) $(MODULE).test; \
	fi
t: test ## Alias for test

# Pattern: make test-Chez or test-CaskChez or test-ChezWiz
.PHONY: test-%
test-%: ## Run tests for a specific module (e.g., test-Chez)
	@$(MILL) $*.test

# Back-compat convenience aliases
.PHONY: tc tca tw
tc: ## Run Chez tests
	@$(MILL) Chez.test
tca: ## Run CaskChez tests
	@$(MILL) CaskChez.test
tw: ## Run ChezWiz tests
	@$(MILL) ChezWiz.test

# README-friendly aliases
.PHONY: test-chez test-caskchez test-chezwiz
test-chez: ## Alias: Run Chez tests
	@$(MILL) Chez.test
test-caskchez: ## Alias: Run CaskChez tests
	@$(MILL) CaskChez.test
test-chezwiz: ## Alias: Run ChezWiz tests
	@$(MILL) ChezWiz.test

# Specific test suite groups (compat with previous targets)
.PHONY: tp td tv ti
tp: ## Run Chez primitives tests
	@$(MILL) Chez.test chez.primitives
td: ## Run Chez derivation tests
	@$(MILL) Chez.test chez.derivation
tv: ## Run Chez validation tests
	@$(MILL) Chez.test chez.validation
ti: ## Run CaskChez integration tests
	@$(MILL) CaskChez.test caskchez.UserCrudAPITest
