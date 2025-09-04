##@ Utils

.PHONY: tree
tree: ## Show trimmed project tree
	@tree -I 'out|.git|.metals|.bloop|.bsp|target|node_modules' -L 2

.PHONY: loc
loc: ## Count Scala lines of code
	@find . -name "*.scala" -not -path "./out/*" | xargs wc -l | tail -1

.PHONY: clean-all purge
clean-all: ## Deep clean (mill + IDE/build caches)
	@$(MILL) clean
	@rm -rf out/ .bloop .bsp .metals target
	@echo "✅ Deep clean complete!"
purge: clean-all ## Alias for clean-all

