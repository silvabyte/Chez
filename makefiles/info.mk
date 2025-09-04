##@ Info

.PHONY: info
info: ## Show project info
	@echo "Chez Project"
	@echo "============"
	@echo "Scala:     3.6.2+"
	@echo "Build:     Mill"
	@echo "Test:      utest"
	@echo "Modules:   Chez (core), CaskChez (web), ChezWiz (AI)"
	@echo ""
	@echo "Run 'make help' for available commands"

.PHONY: version
version: ## Show mill version
	@$(MILL) version

