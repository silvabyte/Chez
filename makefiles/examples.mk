##@ Examples

# Defaults for example endpoints
WEB_HOST ?= localhost
WEB_PORT ?= 8080

# High-level groups
.PHONY: examples examples-all
examples: examples-schema ## Run core schema examples
examples-all: examples-schema example-web-openapi examples-ai ## Run all example groups

# schema (core library) examples
.PHONY: examples-schema example-schema-basic example-schema-complex example-schema-validation example-schema-derivation example-schema-annotations example-schema-enum example-schema-sealed
examples-schema: ## Run common schema examples (basic, complex, validation)
	@$(MILL) schema.runMain boogieloops.schema.examples.BasicUsage
	@$(MILL) schema.runMain boogieloops.schema.examples.ComplexTypes
	@$(MILL) schema.runMain boogieloops.schema.examples.Validation
example-schema-basic: ## Run schema BasicUsage example
	@$(MILL) schema.runMain boogieloops.schema.examples.BasicUsage
example-schema-complex: ## Run schema ComplexTypes example
	@$(MILL) schema.runMain boogieloops.schema.examples.ComplexTypes
example-schema-validation: ## Run schema Validation example
	@$(MILL) schema.runMain boogieloops.schema.examples.Validation
example-schema-derivation: ## Run schema MirrorDerivedExamples
	@$(MILL) schema.runMain boogieloops.schema.examples.MirrorDerivedExamples
example-schema-annotations: ## Run schema AnnotationExample
	@$(MILL) schema.runMain boogieloops.schema.examples.AnnotationExample
example-schema-enum: ## Run schema EnumExample
	@$(MILL) schema.runMain boogieloops.schema.examples.EnumExample
example-schema-sealed: ## Run schema SealedTraitExample
	@$(MILL) schema.runMain boogieloops.schema.examples.SealedTraitExample

# web (HTTP/API) examples
.PHONY: example-web-api example-web-upload example-web-openapi
example-web-api: ## Start web User CRUD API server (blocks)
	@$(MILL) web.runMain boogieloops.web.examples.UserCrudAPI
example-web-upload: ## Start web Upload/Streaming demo server (blocks)
	@$(MILL) web.runMain boogieloops.web.examples.UploadStreamingServer
.PHONY: example-web-upload-curl example-web-upload-curl-upload example-web-upload-curl-stream example-web-upload-curl-decorated
example-web-upload-curl: ## Run demo curl requests against the Upload/Streaming server
	@$(MAKE) example-web-upload-curl-upload
	@$(MAKE) example-web-upload-curl-stream
	@$(MAKE) example-web-upload-curl-decorated
example-web-upload-curl-upload: ## Curl: multipart upload demo
	@echo "🔸 Multipart upload -> http://$(WEB_HOST):$(WEB_PORT)/demo/upload"
	@TMP=$$(mktemp); echo "hello from boogieloops" > $$TMP; \
	  curl -sS -i -X POST \
	    -F file=@$$TMP \
	    -F note=hello \
	    http://$(WEB_HOST):$(WEB_PORT)/demo/upload; \
	  rm -f $$TMP
example-web-upload-curl-stream: ## Curl: streaming response demo
	@echo "🔸 Streaming 1024 bytes -> http://$(WEB_HOST):$(WEB_PORT)/demo/stream/1024"
	@curl -sS -i http://$(WEB_HOST):$(WEB_PORT)/demo/stream/1024
example-web-upload-curl-decorated: ## Curl: decorated route demo (shows custom headers)
	@echo "🔸 Decorated route -> http://$(WEB_HOST):$(WEB_PORT)/demo/decorated"
	@curl -sS -i -H "Accept-Encoding: gzip" http://$(WEB_HOST):$(WEB_PORT)/demo/decorated
example-web-openapi: ## Run only the OpenAPI generator example
	@$(MILL) web.runMain boogieloops.web.examples.OpenAPITest

# ai (LLM agents) examples
.PHONY: examples-ai example-ai-openai example-ai-anthropic example-ai-local
examples-ai: ## Run ai bundled examples (requires API keys)
	@$(MILL) ai.runMain boogieloops.ai.examples.Examples
example-ai-openai: ## Run ai OpenAI provider examples
	@$(MILL) ai.runMain boogieloops.ai.examples.OpenAIExample
example-ai-anthropic: ## Run ai Anthropic provider examples
	@$(MILL) ai.runMain boogieloops.ai.examples.AnthropicExample
example-ai-local: ## Run ai OpenAI-compatible local provider examples
	@$(MILL) ai.runMain boogieloops.ai.examples.OpenAICompatibleExample

# Back-compat convenience aliases (from README / previous Makefile)
.PHONY: schema web web-upload web-curl ai ai-demo ai-openai ai-anthropic ai-local
schema: examples-schema ## Alias: Run schema examples
web: example-web-api ## Alias: Run web API server example
web-upload: example-web-upload ## Alias: Run web Upload/Streaming demo server
web-curl: example-web-upload-curl ## Alias: Run curl demos against Upload/Streaming server
ai: examples-ai ## Alias: Run ai examples bundle
ai-demo: examples-ai ## Alias: Run ai examples bundle
ai-openai: example-ai-openai ## Alias: Run ai OpenAI examples
ai-anthropic: example-ai-anthropic ## Alias: Run ai Anthropic examples
ai-local: example-ai-local ## Alias: Run ai local examples

# Zero to App quickstart (web)
.PHONY: example-web-zeroapp
example-web-zeroapp: ## Start Zero to App quickstart server (blocks)
	@$(MILL) web.runMain boogieloops.web.examples.zerotoapp.ZeroToAppApi
