# Zero to App (10-15 minutes)

Build a tiny, validated HTTP app using the BoogieLoops ecosystem.

What you'll build:

- Define a `User` model with schema (schema + validation)
- Create a minimal API with web (POST/GET)
- Test with curl
- Optional: add a tiny ai endpoint for structured AI output

Prereqs

- Scala 3.6.2+, JDK 17+
- Mill launcher in repo (`./mill`)
- Run `make help` to see commands

1. Create a User model (schema)

Open `web/src/main/scala/boogieloops/examples/zerotoapp/Models.scala` and add/update:

```scala
// File: schema/src/main/scala/quickstart/Models.scala
package quickstart

import boogieloops.schema.derivation.Schema

@Schema.title("CreateUser")
case class CreateUser(
  @Schema.minLength(1) name: String,
  @Schema.format("email") email: String,
  @Schema.minimum(0) age: Int
) derives Schema

@Schema.title("User")
case class User(
  id: String,
  name: String,
  email: String,
  age: Int
) derives Schema
```

2. Add a minimal API (web)

Open `web/src/main/scala/boogieloops/examples/zerotoapp/Api.scala`. The basic POST/GET endpoints are scaffolded; proceed to run it.

3. Run it

```bash
make example-web-zeroapp
```

4. Test with curl

```bash
# Create a user (validated by schema)
curl -s -X POST localhost:8082/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@lovelace.org","age":28}' | jq

# List users
curl -s localhost:8082/users | jq

# Try an invalid payload (should return a validation error)
curl -s -X POST localhost:8082/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"","email":"nope","age":-5}' | jq
```

5. Serve OpenAPI (optional)

Open `web/src/main/scala/boogieloops/examples/zerotoapp/Api.scala` and add this endpoint method:

```scala
// Add this endpoint to quickstart.Api
import boogieloops.web.openapi.config.OpenAPIConfig

@Web.swagger(
  "/openapi",
  OpenAPIConfig(
    title = "Quickstart API",
    summary = Some("Zero to App demo"),
    description = "Auto-generated from RouteSchema",
    version = "1.0.0"
  )
)
def openapi(): String = "" // auto-generated spec
```

Example:

```bash
curl -s http://localhost:8082/openapi | jq '.info.title, .openapi'
```

6. Optional: Add a tiny ai endpoint

```scala
// File: ai/src/main/scala/quickstart/Wiz.scala
package quickstart

import boogieloops.ai.*
import boogieloops.ai.providers.OpenAIProvider
import boogieloops.schema.derivation.Schema
import upickle.default.*

case class Summary(@Schema.minLength(1) text: String) derives Schema, ReadWriter

object Wiz {
  lazy val agent = Agent(
    name = "Summarizer",
    instructions = "Summarize briefly.",
    provider = new OpenAIProvider(sys.env("OPENAI_API_KEY")),
    model = "gpt-4o-mini"
  )
}
```

Wire it into the API:

```scala
// In quickstart.Api
@Web.post(
  "/summaries",
  RouteSchema(
    summary = Some("Summarize text"),
    body = Some(Schema[Summary]),
    responses = Map(200 -> ApiResponse("OK", Schema[Summary]))
  )
)
def summarize(req: ValidatedRequest) = {
  req.getBody[Summary] match {
    case Right(in) =>
      Wiz.agent.generateObject[Summary]("Summarize: " + in.text, RequestMetadata()) match {
        case Right(obj) => write(obj.data)
        case Left(err)  => write(ujson.Obj("error" -> err.toString))
      }
    case Left(err) => write(ujson.Obj("error" -> err.message))
  }
}
```

Run again and POST to `/summaries` (requires `OPENAI_API_KEY`), or point Wiz at a local OpenAI-compatible server.

7. Infer Interests (no AI required)

The Zero-to-App server includes an endpoint that normalizes interests from a free-text profile summary. This version is AI-free by default and can later be swapped to an ai Agent.

```bash
# Create a user first (if you haven't already)
curl -s -X POST localhost:8082/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@lovelace.org","age":28}' | jq

# Infer interests for user id 1 from a profile summary
curl -s -X POST localhost:8082/users/1/interests/infer \
  -H 'Content-Type: application/json' \
  -d '{"text":"Scala backend engineer into APIs, functional programming, ML and DevOps."}' | jq

# Example output (shape):
# {
#   "primary": ["scala","backend","api"],
#   "secondary": ["functional","ml","devops"],
#   "tags": ["scala","backend","api","functional","ml","devops"],
#   "notes": null
# }
```

Troubleshooting

- 400s: ensure `Content-Type: application/json` and valid JSON
- Port in use: change `override def port` or free 8082
- ai: set `OPENAI_API_KEY` or use an OpenAI-compatible local endpoint (LM Studio/Ollama)

Next

- Explore module guides: schema (docs/schema.md), web (docs/web.md), ai (docs/ai.md)
- See `make help` for useful dev commands
