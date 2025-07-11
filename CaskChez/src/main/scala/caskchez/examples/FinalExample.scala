package caskchez.examples

import cask._
import _root_.chez._
import caskchez._
import caskchez.chez
import caskchez.chez.ValidatedRequestReader
import caskchez.openapi.config.OpenAPIConfig
import upickle.default._

/**
 * Final example demonstrating the cleanest approach with custom endpoints
 *
 * This shows how custom Cask endpoints can eliminate validation boilerplate while providing automatic schema validation.
 */
object FinalExample extends cask.MainRoutes {

  override def port = 8082
  override def host = "0.0.0.0"

  // Data types
  case class CreateUserRequest(name: String, email: String, age: Int) derives ReadWriter
  case class UserResponse(id: String, name: String, email: String, age: Int) derives ReadWriter

  // Schema definitions
  val createUserBodySchema = Chez.Object(
    "name" -> Chez.String(minLength = Some(1), maxLength = Some(100)),
    "email" -> Chez.String(format = Some("email")),
    "age" -> Chez.Integer(minimum = Some(0), maximum = Some(150))
  )

  val userResponseSchema = Chez.Object(
    "id" -> Chez.String(),
    "name" -> Chez.String(),
    "email" -> Chez.String(),
    "age" -> Chez.Integer()
  )

  val errorResponseSchema = Chez.Object(
    "error" -> Chez.String(),
    "message" -> Chez.String(),
    "details" -> Chez.Array(Chez.Object()).optional
  )

  // Complete Fastify-like route schema
  val createUserRouteSchema = RouteSchema(
    summary = Some("Create a new user"),
    description = Some("Creates a new user with validation for name, email, and age"),
    tags = List("users"),
    body = Some(createUserBodySchema),
    responses = Map(
      201 -> ApiResponse("User created successfully", userResponseSchema),
      400 -> ApiResponse("Validation error", errorResponseSchema),
      500 -> ApiResponse("Internal server error", errorResponseSchema)
    )
  )

  @chez.get(
    "/",
    RouteSchema(
      responses = Map(
        200 -> ApiResponse("Hello", Chez.String())
      )
    )
  )
  def hello(): String = "Hello from Final CaskChez Example!"

  @chez.get(
    "/health",
    RouteSchema(
      responses = Map(
        200 -> ApiResponse(
          "Healthy",
          Chez.Object(
            "status" -> Chez.String(),
            "timestamp" -> Chez.Integer(),
            "service" -> Chez.String(),
            "approach" -> Chez.String()
          )
        )
      )
    )
  )
  def health(): String = {
    write(
      ujson.Obj(
        "status" -> "healthy",
        "timestamp" -> System.currentTimeMillis(),
        "service" -> "Final CaskChez Example",
        "approach" -> "Custom endpoints with automatic validation"
      )
    )
  }

  @chez.post("/users", createUserRouteSchema)
  def createUser(validatedRequest: ValidatedRequest): cask.Response[String] = {
    validatedRequest.getBody[CreateUserRequest] match {
      case Right(userData) =>
        val response = UserResponse(
          id = s"user_${System.currentTimeMillis()}",
          name = userData.name,
          email = userData.email,
          age = userData.age
        )
        cask.Response(
          write(response),
          statusCode = 201,
          headers = Seq("Content-Type" -> "application/json")
        )
      case Left(error) =>
        cask.Response(
          write(
            ujson.Obj(
              "error" -> "Validation failed",
              "message" -> error.message,
              "details" -> ujson.Arr()
            )
          ),
          statusCode = 400,
          headers = Seq("Content-Type" -> "application/json")
        )
    }
  }

  // OpenAPI 3.1.1 specification endpoint - Import issue to be resolved
  // @chez.swagger("/openapi", OpenAPIConfig(...))
  // def openapi(): String = ""

  // Schema introspection - dynamically shows all registered schemas
  @cask.get("/schemas")
  def getSchemas(): ujson.Value = {
    val routes = RouteSchemaRegistry.getAll
    val schemasList = routes.map { case (key, schema) =>
      key -> ujson.Obj(
        "summary" -> schema.summary.getOrElse(""),
        "description" -> schema.description.getOrElse(""),
        "tags" -> ujson.Arr(schema.tags.map(ujson.Str.apply)*),
        "bodySchema" -> schema.body.map(_.toJsonSchema).getOrElse(ujson.Null),
        "querySchema" -> schema.query.map(_.toJsonSchema).getOrElse(ujson.Null),
        "headersSchema" -> schema.headers.map(_.toJsonSchema).getOrElse(ujson.Null),
        "paramsSchema" -> schema.params.map(_.toJsonSchema).getOrElse(ujson.Null),
        "responses" -> ujson.Obj.from(
          schema.responses.map { case (code, response) =>
            code.toString -> ujson.Obj(
              "description" -> response.description,
              "schema" -> response.schema.toJsonSchema
            )
          }
        )
      )
    }
    ujson.Obj(
      "schemas" -> ujson.Obj.from(schemasList),
      "totalSchemas" -> routes.size,
      "description" -> "Complete schemas for all registered routes with validation details"
    )
  }

  // View all registered routes from the RouteSchemaRegistry
  @cask.get("/routes")
  def getRegisteredRoutes(): ujson.Value = {
    val routes = RouteSchemaRegistry.getAll
    val routesList = routes.map { case (key, schema) =>
      ujson.Obj(
        "route" -> key,
        "bodySchema" -> schema.body.map(_.toJsonSchema).getOrElse(ujson.Null),
        "description" -> schema.description.getOrElse(""),
        "tags" -> ujson.Arr(schema.tags.map(ujson.Str.apply)*)
      )
    }
    ujson.Obj(
      "registeredRoutes" -> ujson.Arr(routesList.toSeq*),
      "totalRoutes" -> routes.size
    )
  }

  println("🚀 Final CaskChez Example Server starting...")
  println("📊 Available endpoints:")
  println("  GET  /                    - Hello message")
  println("  GET  /health              - Health check")
  println("  POST /users               - Create user (basic for now)")
  println("  POST /users/manual        - Create user (manual validation for comparison)")
  println("  GET  /error-demo          - Example data for testing")
  println("  GET  /schemas             - View complete route schema definitions")
  println("  GET  /routes              - View all registered routes from RouteSchemaRegistry")
  println()
  println("🔧 Test commands:")
  println("  # Test endpoints")
  println("  curl http://0.0.0.0:8080/health")
  println("  curl -X POST http://0.0.0.0:8080/users -H 'Content-Type: application/json' -d '{}'")
  println(s"Server started on http://${host}:${port}")

  initialize()
}
