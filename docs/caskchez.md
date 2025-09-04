# CaskChez: Schema‑First HTTP Validation for Cask

Type‑safe, annotation‑driven request validation and OpenAPI 3.1.1 for the Cask web framework. Define schemas once; get body, query, headers, and path validation with structured errors and auto‑generated docs.

## Features

- Annotation‑driven routes: `@CaskChez.get|post|put|patch|delete`
- Automatic request validation: body, query, headers, and path params
- Typed access: `ValidatedRequest.getBody[T]`, `getQuery[T]`, `getQueryParam(name)`
- Route registry: centralized `RouteSchemaRegistry` for introspection/OpenAPI
- OpenAPI 3.1.1: serve specs with `@CaskChez.swagger("/openapi", OpenAPIConfig(...))`

## Install

- Mill:

```scala
mvn"com.silvabyte::chez:0.2.0"
mvn"com.silvabyte::caskchez:0.2.0"
```

- SBT:

```scala
libraryDependencies ++= Seq(
  "com.silvabyte" %% "chez" % "0.2.0",
  "com.silvabyte" %% "caskchez" % "0.2.0"
)
```

## Quickstart (5 minutes)

1) Define models with Chez annotations and upickle codecs

```scala
import chez.derivation.Schema
import upickle.default.*

case class CreateUser(
  @Schema.minLength(1) name: String,
  @Schema.format("email") email: String,
  @Schema.minimum(0) age: Int
) derives Schema, ReadWriter

case class User(id: String, name: String, email: String, age: Int) derives Schema, ReadWriter
```

2) Add a validated endpoint

```scala
import cask.main.Main
import caskchez.*

object Api extends Main {
  @CaskChez.post(
    "/users",
    RouteSchema(
      summary = Some("Create user"),
      body = Some(Schema[CreateUser]),
      responses = Map(201 -> ApiResponse("Created", Schema[User]))
    )
  )
  def create(req: ValidatedRequest) = {
    req.getBody[CreateUser] match {
      case Right(in) => write(User("1", in.name, in.email, in.age))
      case Left(err) => write(ujson.Obj("error" -> "validation_failed", "message" -> err.message))
    }
  }

  override def port = 8082
  initialize()
}
```

3) Run and test

```bash
./mill CaskChez.runMain caskchez.examples.UserCrudAPI   # full example server
# or run your Api object with mill if you add one

curl -s http://localhost:8082/health
curl -s -X POST http://localhost:8082/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ada","email":"ada@lovelace.org","age":28}'
```

## How It Works

- Define a `RouteSchema` alongside your handler; decorate with `@CaskChez.<method>("/path", schema)`.
- On request, CaskChez validates body/query/headers/path against the schema and constructs a `ValidatedRequest`.
- Handlers receive `ValidatedRequest` to access typed data using upickle:
  - `getBody[T: ReadWriter]`, `getQuery[T: ReadWriter]`
  - `getQueryParam(name)`, `getParam(name)`, `getHeader(name)`
- Responses: first successful `responses` status in your `RouteSchema` is used as the status code wrapper for GET/PUT/PATCH/DELETE. Body content is whatever you return (commonly `write(model)`).

## RouteSchema Essentials

- Body: `body = Some(Schema[CreateUser])`
- Query: `query = Some(Schema[MyQuery])`
- Params: `params = Some(Schema[MyPathParams])`
- Headers: `headers = Some(Schema[MyHeaders])`
- Responses: `responses = Map(200 -> ApiResponse("OK", Schema[Out]))`

Minimal example:

```scala
@CaskChez.get(
  "/users",
  RouteSchema(
    summary = Some("List users"),
    query = Some(Schema[UserListQuery]),
    responses = Map(200 -> ApiResponse("OK", Schema[UserListResponse]))
  )
)
def list(r: ValidatedRequest) = write(UserListResponse(Nil, 0, 1, 10, 0))
```

## OpenAPI 3.1.1

- Serve a live OpenAPI document from registered routes:

```scala
import caskchez.openapi.config.OpenAPIConfig

@CaskChez.swagger(
  "/openapi",
  OpenAPIConfig(
    title = "User API",
    summary = Some("User management with validation"),
    description = "Auto‑generated from RouteSchema",
    version = "1.0.0"
  )
)
def openapi(): String = ""  // auto‑generated JSON
```

## Error Shape

- Invalid requests return a structured JSON error:

```json
{
  "error": "Validation failed",
  "message": "Request validation failed",
  "details": [
    { "type": "RequestBodyError", "message": "Missing required field: email", "path": "/body", "field": "email" }
  ]
}
```

## Recipes

- Typed body access: `req.getBody[In]` → `Either[ValidationError, In]`
- Typed query object: `req.getQuery[Q]`
- Single query param: `req.getQueryParam("limit").map(_.toInt)`
- Path params via method args: `def get(id: String, req: ValidatedRequest)`
- Introspect schemas: `RouteSchemaRegistry.getAll`

## Testing

- Run module tests: `./mill CaskChez.test` or `make test-caskchez`
- Example server: `./mill CaskChez.runMain caskchez.examples.UserCrudAPI`
- Single suite: `./mill CaskChez.test caskchez.UserCrudAPITest`

## Troubleshooting

- Content‑Type: send `Content-Type: application/json` for request bodies.
- Port in use: example server listens on `8082`; change `override def port` or stop the other process.
- Body parse errors: ensure valid JSON; errors surface under `RequestBodyError` with a parse message.
- Unexpected 200/204 status: GET/PUT/PATCH/DELETE use the first success response status in `responses`.

## Examples

- Full CRUD with OpenAPI: `CaskChez/src/main/scala/caskchez/examples/UserCrudAPI.scala`
- OpenAPI generation sample: `CaskChez/src/main/scala/caskchez/examples/OpenAPITest.scala`
