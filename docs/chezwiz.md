# ChezWiz: Type‑Safe LLM Agents

LLM agents with multi‑provider support (OpenAI, Anthropic, OpenAI‑compatible local endpoints), typed structured output using Chez schemas, scoped history, and hooks/metrics.

## Install

Mill:

```scala
mvn"com.silvabyte::chez:0.3.0"
mvn"com.silvabyte::chezwiz:0.3.0"
```

SBT:

```scala
libraryDependencies ++= Seq(
  "com.silvabyte" %% "chez" % "0.3.0",
  "com.silvabyte" %% "chezwiz" % "0.3.0"
)
```

## Quickstart

```scala
import chezwiz.agent.*
import chezwiz.agent.providers.OpenAIProvider
import chez.derivation.Schema

@Schema.title("Summary")
case class Summary(@Schema.minLength(1) text: String) derives Schema

val agent = Agent(
  name = "Summarizer",
  instructions = "Summarize briefly.",
  provider = new OpenAIProvider(sys.env("OPENAI_API_KEY")),
  model = "gpt-4o-mini"
)

val md = RequestMetadata(userId = Some("dev"))
val res = agent.generateObject[Summary]("Summarize: Scala 3 match types.", md)
```

## Local LLMs (OpenAI‑Compatible)

```scala
import chezwiz.agent.providers.OpenAICompatibleProvider

val local = OpenAICompatibleProvider(
  baseUrl = "http://localhost:1234/v1",
  modelId = "local-model",
  strictModelValidation = false
)

val agent = Agent("Local", "You are helpful.", local, model = "local-model")
```

## Structured Output

- Define your data with Chez annotations; use `agent.generateObject[T]`.
- On success you get `Right(ObjectResponse[T])`; failures return `Left(ChezError)`.

## Metadata & Scoping

- Provide `RequestMetadata(tenantId, userId, conversationId)` to scope history.
- Use `generateTextWithoutHistory` for stateless calls.

→ [Detailed Guide: Metadata & Scoping](./chezwiz/metadata-and-scoping.md)

## Hooks & Metrics

- Register hooks for logging/metrics/tracing; combine multiple hooks.
- Built‑in metrics helpers available; export to Prometheus or JSON if desired.

→ [Detailed Guide: Hooks & Metrics](./chezwiz/hooks-and-metrics.md)

## Run Examples

- `make wiz` (requires `OPENAI_API_KEY` or a local LLM endpoint)
- Local LLM: ensure your server exposes `/v1` and a valid model ID.
