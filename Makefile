################################################################################
# BoogieLoops Project Makefile (modular)
#
# This top-level Makefile delegates real logic to files in makefiles/.
# It provides a clean help output and variables you can override.
################################################################################

.DEFAULT_GOAL := help
SHELL := /bin/bash
.SHELLFLAGS := -eu -o pipefail -c

# Core variables (override via `make VAR=...`)
# Path to mill launcher
MILL ?= ./mill
# Target module: one of `__`, `Schema`, `Web`, `AI`
MODULE ?= __
# Optional test suite/class to run (e.g., boogieloops.schema.primitives.StringSchemaTests)
SUITE ?=
# Set to 1 to run in watch mode where supported
WATCH ?= 0

# Include modular makefiles
MAKEFILES_DIR := makefiles
-include $(MAKEFILES_DIR)/common.mk
-include $(MAKEFILES_DIR)/build.mk
-include $(MAKEFILES_DIR)/test.mk
-include $(MAKEFILES_DIR)/watch.mk
-include $(MAKEFILES_DIR)/examples.mk
-include $(MAKEFILES_DIR)/dev.mk
-include $(MAKEFILES_DIR)/ci.mk
-include $(MAKEFILES_DIR)/info.mk
-include $(MAKEFILES_DIR)/util.mk
-include $(MAKEFILES_DIR)/sdk.mk
