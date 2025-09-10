##@ Examples

# Defaults for example endpoints
CASKCHEZ_HOST ?= localhost
CASKCHEZ_PORT ?= 8080

# High-level groups
.PHONY: examples examples-all
examples: examples-chez ## Run core Chez examples
examples-all: examples-chez example-caskchez-openapi examples-chezwiz ## Run all example groups

# Chez (core library) examples
.PHONY: examples-chez example-chez-basic example-chez-complex example-chez-validation example-chez-derivation example-chez-annotations example-chez-enum example-chez-sealed
examples-chez: ## Run common Chez examples (basic, complex, validation)
	@$(MILL) Chez.runMain chez.examples.BasicUsage
	@$(MILL) Chez.runMain chez.examples.ComplexTypes
	@$(MILL) Chez.runMain chez.examples.Validation
example-chez-basic: ## Run Chez BasicUsage example
	@$(MILL) Chez.runMain chez.examples.BasicUsage
example-chez-complex: ## Run Chez ComplexTypes example
	@$(MILL) Chez.runMain chez.examples.ComplexTypes
example-chez-validation: ## Run Chez Validation example
	@$(MILL) Chez.runMain chez.examples.Validation
example-chez-derivation: ## Run Chez MirrorDerivedExamples
	@$(MILL) Chez.runMain chez.examples.MirrorDerivedExamples
example-chez-annotations: ## Run Chez AnnotationExample
	@$(MILL) Chez.runMain chez.examples.AnnotationExample
example-chez-enum: ## Run Chez EnumExample
	@$(MILL) Chez.runMain chez.examples.EnumExample
example-chez-sealed: ## Run Chez SealedTraitExample
	@$(MILL) Chez.runMain chez.examples.SealedTraitExample

# CaskChez (web/API) examples
.PHONY: example-caskchez-api example-caskchez-upload example-caskchez-openapi
example-caskchez-api: ## Start CaskChez User CRUD API server (blocks)
	@$(MILL) CaskChez.runMain caskchez.examples.UserCrudAPI
example-caskchez-upload: ## Start CaskChez Upload/Streaming demo server (blocks)
	@$(MILL) CaskChez.runMain caskchez.examples.UploadStreamingServer
.PHONY: example-caskchez-upload-curl example-caskchez-upload-curl-upload example-caskchez-upload-curl-stream example-caskchez-upload-curl-decorated
example-caskchez-upload-curl: ## Run demo curl requests against the Upload/Streaming server
	@$(MAKE) example-caskchez-upload-curl-upload
	@$(MAKE) example-caskchez-upload-curl-stream
	@$(MAKE) example-caskchez-upload-curl-decorated
example-caskchez-upload-curl-upload: ## Curl: multipart upload demo
	@echo "🔸 Multipart upload -> http://$(CASKCHEZ_HOST):$(CASKCHEZ_PORT)/demo/upload"
	@TMP=$$(mktemp); echo "hello from caskchez" > $$TMP; \
	  curl -sS -i -X POST \
	    -F file=@$$TMP \
	    -F note=hello \
	    http://$(CASKCHEZ_HOST):$(CASKCHEZ_PORT)/demo/upload; \
	  rm -f $$TMP
example-caskchez-upload-curl-stream: ## Curl: streaming response demo
	@echo "🔸 Streaming 1024 bytes -> http://$(CASKCHEZ_HOST):$(CASKCHEZ_PORT)/demo/stream/1024"
	@curl -sS -i http://$(CASKCHEZ_HOST):$(CASKCHEZ_PORT)/demo/stream/1024
example-caskchez-upload-curl-decorated: ## Curl: decorated route demo (shows custom headers)
	@echo "🔸 Decorated route -> http://$(CASKCHEZ_HOST):$(CASKCHEZ_PORT)/demo/decorated"
	@curl -sS -i -H "Accept-Encoding: gzip" http://$(CASKCHEZ_HOST):$(CASKCHEZ_PORT)/demo/decorated
example-caskchez-openapi: ## Run only the OpenAPI generator example
	@$(MILL) CaskChez.runMain caskchez.examples.OpenAPITest

# ChezWiz (LLM agents) examples
.PHONY: examples-chezwiz example-chezwiz-openai example-chezwiz-anthropic example-chezwiz-local
examples-chezwiz: ## Run ChezWiz bundled examples (requires API keys)
	@$(MILL) ChezWiz.runMain chezwiz.agent.examples.Examples
example-chezwiz-openai: ## Run ChezWiz OpenAI provider examples
	@$(MILL) ChezWiz.runMain chezwiz.agent.examples.OpenAIExample
example-chezwiz-anthropic: ## Run ChezWiz Anthropic provider examples
	@$(MILL) ChezWiz.runMain chezwiz.agent.examples.AnthropicExample
example-chezwiz-local: ## Run ChezWiz OpenAI-compatible local provider examples
	@$(MILL) ChezWiz.runMain chezwiz.agent.examples.OpenAICompatibleExample

# Back-compat convenience aliases (from README / previous Makefile)
.PHONY: chez caskchez caskchez-upload caskchez-curl wiz wiz-demo wiz-openai wiz-anthropic wiz-local
chez: examples-chez ## Alias: Run Chez examples
caskchez: example-caskchez-api ## Alias: Run CaskChez API server example
caskchez-upload: example-caskchez-upload ## Alias: Run CaskChez Upload/Streaming demo server
 caskchez-curl: example-caskchez-upload-curl ## Alias: Run curl demos against Upload/Streaming server
wiz: examples-chezwiz ## Alias: Run ChezWiz examples bundle
wiz-demo: examples-chezwiz ## Alias: Run ChezWiz examples bundle
wiz-openai: example-chezwiz-openai ## Alias: Run ChezWiz OpenAI examples
wiz-anthropic: example-chezwiz-anthropic ## Alias: Run ChezWiz Anthropic examples
wiz-local: example-chezwiz-local ## Alias: Run ChezWiz local examples

# Zero to App quickstart (CaskChez)
.PHONY: example-caskchez-zeroapp
example-caskchez-zeroapp: ## Start Zero to App quickstart server (blocks)
	@$(MILL) CaskChez.runMain caskchez.examples.zerotoapp.ZeroToAppApi
