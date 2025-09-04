##@ Dev

.PHONY: assembly
assembly: ## Build assembly jars for all modules
	@$(MILL) __.assembly

.PHONY: docs
docs: ## Generate documentation jars
	@$(MILL) __.docJar

.PHONY: deps
deps: ## Show dependency updates
	@$(MILL) mill.scalalib.Dependency/showUpdates

