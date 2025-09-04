##@ Build

.PHONY: build b
build: ## Compile modules (override MODULE=Chez|CaskChez|ChezWiz)
	@$(MILL) $(MODULE).compile
b: build ## Alias for build

.PHONY: clean
clean: ## Clean build artifacts
	@$(MILL) clean
	@rm -rf out/

.PHONY: format fmt
format: ## Format code with scalafmt
	@$(MILL) mill.scalalib.scalafmt.ScalafmtModule/reformatAll
fmt: format ## Alias for format

.PHONY: format-check
format-check: ## Check scalafmt without rewriting
	@$(MILL) mill.scalalib.scalafmt.ScalafmtModule/check

.PHONY: lint fix
lint: ## Run scalafix (lint/fixes)
	@$(MILL) __.fix
fix: lint ## Alias for lint

