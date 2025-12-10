##@ Info

.PHONY: info
info: ## Show project info
	@echo "Chez Project"
	@echo "============"
	@echo "Scala:     3.6.2+"
	@echo "Build:     Mill"
	@echo "Test:      utest"
	@echo "Modules:   Chez (core), Web (web), ChezWiz (AI)"
	@echo ""
	@echo "Run 'make help' for available commands"

.PHONY: version
version: ## Show project version (from build)
	@printf "Project version: %s\n" "$$($(MILL) show Chez.publishVersion | sed -n 's/.*"\(.*\)".*/\1/p' | tail -n1)"

.PHONY: mill-version
mill-version: ## Show Mill version
	@$(MILL) version

.PHONY: versions
versions: ## Show project, Scala, and Mill versions
	@printf "Project: %s\n" "$$($(MILL) show Chez.publishVersion | sed -n 's/.*"\(.*\)".*/\1/p' | tail -n1)"
	@printf "Scala:   %s\n" "$$($(MILL) show Chez.scalaVersion | sed -n 's/.*"\(.*\)".*/\1/p' | tail -n1)"
	@printf "Mill:    %s\n" "$$($(MILL) version | tail -n1)"
