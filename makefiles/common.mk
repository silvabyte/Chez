##@ Core

# Auto help generator: parses lines with '##' descriptions
.PHONY: help
help: ## Show this help with available targets
	@echo "BoogieLoops Project Commands"
	@echo "============================"
	@awk 'BEGIN {FS = ":.*##"} \
	  /^##@/ { printf "\n%s\n", substr($$0, 5); next } \
	  /^[a-zA-Z0-9_.\-]+:.*##/ { printf "  %-22s %s\n", $$1, $$2 } \
	' $(MAKEFILE_LIST)

