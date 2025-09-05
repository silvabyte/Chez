# Getting Started

Welcome to the Chez ecosystem — three Scala 3 libraries that work great together or stand alone:

- Chez: Type‑safe JSON Schema (derive, build, validate)
- CaskChez: Schema‑first HTTP validation + OpenAPI for Cask
- ChezWiz: Type‑safe LLM agents with structured output

## Prerequisites

- Scala 3.6.2+
- JDK 17+
- Mill (launcher included as `./mill`)

## Install

Mill:

```scala
mvn"com.silvabyte::chez:0.3.0"        // Core schemas
mvn"com.silvabyte::caskchez:0.3.0"    // HTTP validation + OpenAPI
mvn"com.silvabyte::chezwiz:0.3.0"     // LLM agents
```

SBT:

```scala
libraryDependencies ++= Seq(
  "com.silvabyte" %% "chez" % "0.3.0",
  "com.silvabyte" %% "caskchez" % "0.3.0",
  "com.silvabyte" %% "chezwiz" % "0.3.0"
)
```

## Common Commands

- Build: `make build` (or `./mill __.compile`)
- Test: `make test` (override `MODULE=Chez|CaskChez|ChezWiz`)
- Lint: `make lint` (scalafix)
- Format: `make format` (scalafmt)
- Watch: `make watch MODULE=Chez` or `make watch-test MODULE=CaskChez`
- Examples: `make chez`, `make caskchez`, `make wiz`

Run `make help` for the full list.

## Examples Overview

- `make chez`: Run core Chez examples
- `make caskchez`: Start the CaskChez API example on port 8082
- `make wiz`: Run ChezWiz examples (requires API keys or a local LLM)

ChezWiz examples environment variables:

```bash
export OPENAI_API_KEY="your-openai-key"           # OpenAI provider
export ANTHROPIC_API_KEY="your-anthropic-key"     # Anthropic provider (optional)
```

Local LLMs via OpenAI‑compatible endpoints (no key by default) are supported (LM Studio, Ollama, llama.cpp).

## Next Steps

- Chez (core): [chez.md](./chez.md)
- CaskChez (web): [caskchez.md](./caskchez.md)
- ChezWiz (AI): [chezwiz.md](./chezwiz.md)

## More Docs

- Concepts: [concepts.md](./concepts.md)
- Zero to App: [zero-to-app.md](./zero-to-app.md)
- Troubleshooting: [troubleshooting.md](./troubleshooting.md)
- TypeScript SDK: [typescript-sdk.md](./typescript-sdk.md)
