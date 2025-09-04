# Getting Started

## Prerequisites

- Scala 3.6.2+
- JDK 17+
- Mill (launcher included as `./mill`)

## Install (Mill / SBT)

Mill:

```scala
mvn"com.silvabyte::chez:0.2.0"        // Core schemas
mvn"com.silvabyte::caskchez:0.2.0"    // HTTP validation + OpenAPI
mvn"com.silvabyte::chezwiz:0.2.0"     // LLM agents
```

SBT:

```scala
libraryDependencies ++= Seq(
  "com.silvabyte" %% "chez" % "0.2.0",
  "com.silvabyte" %% "caskchez" % "0.2.0",
  "com.silvabyte" %% "chezwiz" % "0.2.0"
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

ChezWiz environment variables:

```bash
export OPENAI_API_KEY="your-openai-key"           # OpenAI provider
export ANTHROPIC_API_KEY="your-anthropic-key"     # Anthropic provider (optional)
```

Local LLMs via OpenAI‑compatible endpoints (no key by default) are supported (LM Studio, Ollama, llama.cpp).

