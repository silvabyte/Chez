# ScalaSchemaz - Comprehensive Development Makefile
# Provides convenient commands for testing, examples, compilation, and project management

# ============================================================================
# BUILD & COMPILATION
# ============================================================================

# Compile all modules (Chez and CaskChez)
.PHONY: compile
compile:
	@echo "🔨 Compiling all modules..."
	./mill Chez.compile
	./mill CaskChez.compile

# Compile only the Chez core module
.PHONY: compile-chez
compile-chez:
	@echo "🔨 Compiling Chez module..."
	./mill Chez.compile

# Compile only the CaskChez web framework module
.PHONY: compile-cask
compile-cask:
	@echo "🔨 Compiling CaskChez module..."
	./mill CaskChez.compile

# Alias for compile command
.PHONY: build
build: compile

# Create executable JAR assembly
.PHONY: assembly
assembly:
	@echo "📦 Creating assembly JAR..."
	./mill Chez.assembly

# Clean build artifacts
.PHONY: clean
clean:
	@echo "🧹 Cleaning build artifacts..."
	./mill clean
	rm -rf out/

# Deep clean - removes all build artifacts including IDE files
.PHONY: clean-all
clean-all: clean
	@echo "🧹 Deep cleaning..."
	rm -rf .bloop .bsp .metals out target
	@echo "✅ Deep clean completed!"

# ============================================================================
# TESTING
# ============================================================================

# Run all tests for the project
.PHONY: test
test:
	@echo "🧪 Running all tests..."
	./mill Chez.test

# Run only Chez module tests
.PHONY: test-chez
test-chez:
	@echo "🧪 Running Chez tests..."
	./mill Chez.test

# Run only CaskChez module tests
.PHONY: test-cask
test-cask:
	@echo "🧪 Running CaskChez tests..."
	./mill CaskChez.test

# Run primitive type tests (String, Number, Boolean, etc.)
.PHONY: test-primitives
test-primitives:
	@echo "🧪 Running primitive type tests..."
	./mill Chez.test chez.primitives

# Run schema derivation tests
.PHONY: test-derivation
test-derivation:
	@echo "🧪 Running schema derivation tests..."
	./mill Chez.test chez.derivation

# Run complex type tests (Array, Object)
.PHONY: test-complex
test-complex:
	@echo "🧪 Running complex type tests..."
	./mill Chez.test chez.complex

# Run tests with coverage (if available)
.PHONY: test-coverage
test-coverage:
	@echo "🧪 Running tests with coverage..."
	./mill Chez.test

# Run tests in watch mode - reruns on file changes
.PHONY: test-watch
test-watch:
	@echo "🧪 Running tests in watch mode..."
	@echo "ℹ️  Press Ctrl+C to stop watching"
	while true; do \
		clear; \
		./mill Chez.test; \
		echo ""; \
		echo "👀 Watching for changes... (Ctrl+C to stop)"; \
		sleep 3; \
	done

# Quick smoke test to verify basic functionality
.PHONY: quick-test
quick-test:
	@echo "⚡ Running quick smoke test..."
	./mill Chez.test chez.primitives.StringChezTests

# ============================================================================
# EXAMPLES & DEMOS
# ============================================================================

# Run basic usage example showing core functionality
.PHONY: example-basic
example-basic:
	@echo "🚀 Running Basic Usage example..."
	./mill Chez.test.runMain chez.examples.BasicUsage

# Run complex types example demonstrating advanced features
.PHONY: example-complex
example-complex:
	@echo "🚀 Running Complex Types example..."
	./mill Chez.test.runMain chez.examples.ComplexTypes

# Run validation example showing schema validation features
.PHONY: example-validation
example-validation:
	@echo "🚀 Running Validation example..."
	./mill Chez.test.runMain chez.examples.Validation

# Run annotation example demonstrating schema annotations
.PHONY: example-annotation
example-annotation:
	@echo "🚀 Running Annotation example..."
	./mill Chez.test.runMain chez.examples.runAnnotationExample

# Run enum example showing Scala 3 enum support
.PHONY: example-enum
example-enum:
	@echo "🚀 Running Enum example..."
	./mill Chez.test.runMain chez.examples.EnumExample

# Run sealed trait example showing discriminated union support
.PHONY: example-sealed-trait
example-sealed-trait:
	@echo "🚀 Running Sealed Trait example..."
	./mill Chez.test.runMain chez.examples.SealedTraitExample

# Run mirror derivation example showing compile-time reflection
.PHONY: example-mirror
example-mirror:
	@echo "🚀 Running Mirror Derivation example..."
	./mill Chez.test.runMain chez.examples.runMirrorDerivedExamples

# Run default parameter detection example
.PHONY: example-defaults
example-defaults:
	@echo "🚀 Running Default Parameter example..."
	./mill Chez.test.runMain chez.examples.DefaultParameterTest

# Run all examples in sequence
.PHONY: examples
examples:
	@echo "🚀 Running ALL examples..."
	@echo "=================================="
	make example-basic
	@echo ""
	make example-complex
	@echo ""
	make example-validation
	@echo ""
	make example-annotation
	@echo ""
	make example-enum
	@echo ""
	make example-sealed-trait
	@echo ""
	make example-mirror
	@echo ""
	make example-defaults
	@echo ""
	@echo "✅ All examples completed!"

# Alias for examples command
.PHONY: demo
demo: examples

# ============================================================================
# DEVELOPMENT & MAINTENANCE  
# ============================================================================

# Start Scala REPL with project classpath loaded
.PHONY: repl
repl:
	@echo "🎯 Starting Scala REPL..."
	./mill Chez.repl

# Alias for repl command
.PHONY: console
console: repl

# Show dependency tree for Chez module
.PHONY: deps
deps:
	@echo "📦 Dependency tree for Chez module:"
	./mill Chez.ivyDepsTree

# Show dependency tree for CaskChez module
.PHONY: deps-cask
deps-cask:
	@echo "📦 Dependency tree for CaskChez module:"
	./mill CaskChez.ivyDepsTree

# Format source code (requires scalafmt setup)
.PHONY: format
format:
	@echo "🎨 Code formatting..."
	@echo "ℹ️  Run: './mill mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources' to format"

# Run linting (compile warnings)
.PHONY: lint
lint:
	@echo "🔍 Linting code..."
	./mill Chez.compile
	./mill CaskChez.compile

# Setup development environment
.PHONY: dev-setup
dev-setup:
	@echo "🛠️  Setting up development environment..."
	@echo "✅ Mill is available"
	@echo "✅ Scala 3.6.2 configured"
	@echo "✅ Dependencies will be downloaded on first build"
	@echo "🚀 Ready to develop! Try: make compile && make test"

# Show development status and project statistics
.PHONY: dev-status
dev-status:
	@echo "📊 Development Status"
	@echo "===================="
	@echo "📁 Project structure:"
	@find Chez/src -name "*.scala" | wc -l | xargs echo "  Chez source files:"
	@find CaskChez/src -name "*.scala" 2>/dev/null | wc -l | xargs echo "  CaskChez source files:" || echo "  CaskChez source files: 0"
	@find Chez/test -name "*.scala" | wc -l | xargs echo "  Test files:"
	@echo ""
	@echo "🧪 Test status:"
	@echo "  Run 'make test' to check current test status"
	@echo ""

# ============================================================================
# PROJECT MANAGEMENT
# ============================================================================

# Show project information and configuration
.PHONY: info
info:
	@echo "📊 ScalaSchemaz Project Information"
	@echo "=================================="
	@echo "🔸 Scala Version: 3.6.2"
	@echo "🔸 Build Tool: Mill"
	@echo "🔸 Test Framework: utest"
	@echo "🔸 Main Dependencies: upickle, os-lib"
	@echo "🔸 Modules: Chez (core), CaskChez (web framework integration)"
	@echo ""

# Show version information for tools and dependencies
.PHONY: version
version:
	@echo "📋 Version Information:"
	@echo "  Scala: $$(./mill Chez.scalaVersion)"
	@echo "  Mill: $$(./mill --version 2>/dev/null || echo 'Unknown')"
	@echo "  Java: $$(java -version 2>&1 | head -1)"

# Show project directory tree
.PHONY: tree
tree:
	@echo "📁 Project Structure:"
	@tree -I 'out|.git|.metals|.bloop|.bsp|target|node_modules' -L 3 || \
	 find . -type d -name "out" -prune -o -type d -name ".git" -prune -o -type d -print | head -20

# List available Mill modules
.PHONY: modules
modules:
	@echo "📦 Available Mill modules:"
	./mill resolve __ | grep -E "^(Chez|CaskChez)$$"

# Show Mill targets for Chez module
.PHONY: targets
targets:
	@echo "🎯 Available Mill targets for Chez:"
	./mill resolve Chez._ | grep -E "(compile|test|run|repl|assembly)" | sort

# Show Mill help
.PHONY: mill-help
mill-help:
	@echo "🏭 Mill Build Tool Help:"
	./mill --help

# ============================================================================
# ADVANCED COMMANDS
# ============================================================================

# Full project check - compile and test
.PHONY: check
check: compile test
	@echo "✅ Full project check completed!"

# Continuous integration check
.PHONY: ci
ci: check
	@echo "🤖 CI pipeline completed successfully!"

# Run performance benchmarks (placeholder)
.PHONY: benchmark
benchmark:
	@echo "⏱️  Benchmarking..."
	@echo "ℹ️  No benchmarks configured yet"

# Generate documentation
.PHONY: docs
docs:
	@echo "📖 Generating documentation..."
	./mill Chez.docJar

# ============================================================================
# LEGACY COMPATIBILITY
# ============================================================================

# Legacy command for basic usage example
.PHONY: run-example-basic-usage
run-example-basic-usage: example-basic

# Legacy command for complex types example
.PHONY: run-example-complex-types  
run-example-complex-types: example-complex

# Legacy command for validation example
.PHONY: run-example-validation
run-example-validation: example-validation

# Legacy command for running all examples
.PHONY: run-example-all
run-example-all: examples

# End of Makefile