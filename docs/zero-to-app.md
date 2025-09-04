# Zero to App (10–15 minutes)

Build a tiny, validated HTTP app using the Chez ecosystem.

What you’ll build:

- Define a `User` model with Chez (schema + validation)
- Create a minimal API with CaskChez (POST/GET)
- Test with curl
- Optional: add a tiny ChezWiz endpoint for structured AI output

Prereqs

- Scala 3.6.2+, JDK 17+
- Mill launcher in repo (`./mill`)
- Run `make help` to see commands

1. Create a User model (Chez)

```scala
// File: Chez/src/main/scala/quickstart/Models.scala
package quickstart

import chez.derivation.Schema

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

2. Add a minimal API (CaskChez)

```scala
// Prefer using the stubs shipped in this repo:
//   CaskChez/src/main/scala/caskchez/examples/zerotoapp/Api.scala
//   CaskChez/src/main/scala/caskchez/examples/zerotoapp/Models.scala
// These stubs compile and run out of the box; open them and follow the TODOs.
```

3. Run it

```bash
make example-caskchez-zeroapp
```

4. Test with curl

```bash
# Create a user (validated by Chez schema)
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

```scala
// Add this endpoint to quickstart.Api
import caskchez.openapi.config.OpenAPIConfig

@CaskChez.swagger(
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

6. Optional: Add a tiny ChezWiz endpoint

```scala
// File: ChezWiz/src/main/scala/quickstart/Wiz.scala
package quickstart

import chezwiz.agent.*
import chezwiz.agent.providers.OpenAIProvider
import chez.derivation.Schema
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
@CaskChez.post(
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

Run again and POST to `/summaries` (requires `OPENAI_API_KEY`), or point Wiz at a local OpenAI‑compatible server.

Troubleshooting

- 400s: ensure `Content-Type: application/json` and valid JSON
- Port in use: change `override def port` or free 8082
- ChezWiz: set `OPENAI_API_KEY` or use an OpenAI‑compatible local endpoint (LM Studio/Ollama)

Next

- Explore module guides: Chez (docs/chez.md), CaskChez (docs/caskchez.md), ChezWiz (docs/chezwiz.md)
- See `make help` for useful dev commands
