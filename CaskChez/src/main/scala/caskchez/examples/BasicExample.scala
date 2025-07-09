package caskchez.examples

import cask._
import chez._
import caskchez._

/**
 * Basic example of CaskChez integration
 *
 * This example demonstrates the basic usage of CaskChez with simple validation and schema definitions without complex decorators.
 */
object BasicExample extends cask.MainRoutes {

  // Simple schema definitions
  val userSchema = Chez.Object(
    "name" -> Chez.String(minLength = Some(1)),
    "email" -> Chez.String(format = Some("email")),
    "age" -> Chez.Integer(minimum = Some(0), maximum = Some(150))
  )

  val errorSchema = Chez.Object(
    "error" -> Chez.String(),
    "message" -> Chez.String()
  )

  // Basic route without decorators for now
  @cask.get("/")
  def hello(): String = {
    "Hello from CaskChez!"
  }

  @cask.get("/health")
  def health(): ujson.Value = {
    ujson.Obj(
      "status" -> "healthy",
      "timestamp" -> System.currentTimeMillis(),
      "service" -> "CaskChez Example"
    )
  }

  @cask.get("/schema")
  def getSchema(): ujson.Value = {
    ujson.Obj(
      "userSchema" -> userSchema.toJsonSchema,
      "errorSchema" -> errorSchema.toJsonSchema
    )
  }

  // Simple validation example
  @cask.postJson("/validate")
  def validateUser(request: cask.Request): cask.Response[ujson.Value] = {
    try {
      val bodyBytes = request.data.readAllBytes()
      val bodyStr = new String(bodyBytes, "UTF-8")
      val bodyJson = ujson.read(bodyStr)

      // Manual validation for now
      bodyJson match {
        case obj: ujson.Obj =>
          val validation = RequestValidator.validateBody(request, userSchema)
          if (validation.isValid) {
            cask.Response(
              ujson.Obj(
                "valid" -> true,
                "message" -> "User data is valid",
                "data" -> bodyJson
              ),
              statusCode = 200
            )
          } else {
            cask.Response(
              ujson.Obj(
                "valid" -> false,
                "message" -> "Validation failed",
                "errors" -> ujson.Arr(validation.errors.map(e => ujson.Str(e.message))*)
              ),
              statusCode = 400
            )
          }
        case _ =>
          cask.Response(
            ujson.Obj(
              "valid" -> false,
              "message" -> "Expected JSON object"
            ),
            statusCode = 400
          )
      }
    } catch {
      case e: Exception =>
        cask.Response(
          ujson.Obj(
            "valid" -> false,
            "message" -> s"Error processing request: ${e.getMessage}"
          ),
          statusCode = 500
        )
    }
  }

  // Example with query parameters
  @cask.get("/users")
  def getUsers(limit: Int = 10, offset: Int = 0): ujson.Value = {
    ujson.Obj(
      "users" -> ujson.Arr(
        ujson.Obj(
          "name" -> "John Doe",
          "email" -> "john@example.com",
          "age" -> 30
        ),
        ujson.Obj(
          "name" -> "Jane Smith",
          "email" -> "jane@example.com",
          "age" -> 25
        )
      ),
      "pagination" -> ujson.Obj(
        "limit" -> limit,
        "offset" -> offset,
        "total" -> 2
      )
    )
  }

  // Route schema registry example
  @cask.get("/api-docs")
  def apiDocs(): ujson.Value = {
    val schemas = RouteSchemaRegistry.getAll

    ujson.Obj(
      "openapi" -> "3.0.0",
      "info" -> ujson.Obj(
        "title" -> "CaskChez Example API",
        "version" -> "1.0.0",
        "description" -> "Example API using CaskChez for schema validation"
      ),
      "paths" -> ujson.Obj.from(
        schemas.map { case (route, schema) =>
          route -> ujson.Obj(
            "description" -> schema.description.getOrElse(""),
            "tags" -> ujson.Arr(schema.tags.map(ujson.Str(_))*)
          )
        }
      ),
      "components" -> ujson.Obj(
        "schemas" -> ujson.Obj(
          "User" -> userSchema.toJsonSchema,
          "Error" -> errorSchema.toJsonSchema
        )
      )
    )
  }

  override def main(args: Array[String]): Unit = {
    // Register some example schemas
    RouteSchemaRegistry.register(
      "/users",
      "GET",
      RouteSchema(
        description = Some("Get users with pagination"),
        tags = List("users"),
        query = Some(
          Chez.Object(
            "limit" -> Chez.Integer(minimum = Some(1), maximum = Some(100)),
            "offset" -> Chez.Integer(minimum = Some(0))
          )
        ),
        responses = Map(
          200 -> ApiResponse(
            "Success",
            Chez.Object(
              "users" -> Chez.Array(userSchema),
              "pagination" -> Chez.Object(
                "limit" -> Chez.Integer(),
                "offset" -> Chez.Integer(),
                "total" -> Chez.Integer()
              )
            )
          )
        )
      )
    )

    RouteSchemaRegistry.register(
      "/validate",
      "POST",
      RouteSchema(
        description = Some("Validate user data"),
        tags = List("users", "validation"),
        body = Some(userSchema),
        responses = Map(
          200 -> ApiResponse(
            "Valid user data",
            Chez.Object(
              "valid" -> Chez.Boolean(),
              "message" -> Chez.String(),
              "data" -> userSchema
            )
          ),
          400 -> ApiResponse("Validation failed", errorSchema)
        )
      )
    )

    println("🚀 CaskChez Example Server starting...")
    println("📊 Available endpoints:")
    println("  GET  /           - Hello message")
    println("  GET  /health     - Health check")
    println("  GET  /schema     - View schemas")
    println("  POST /validate   - Validate user data")
    println("  GET  /users      - Get users with pagination")
    println("  GET  /api-docs   - OpenAPI documentation")
    println()
    println("🔧 Example curl commands:")
    println("  curl http://localhost:8080/")
    println("  curl http://localhost:8080/health")
    println("  curl http://localhost:8080/schema")
    println(
      "  curl -X POST http://localhost:8080/validate -H 'Content-Type: application/json' -d '{\"name\":\"John\",\"email\":\"john@example.com\",\"age\":30}'"
    )
    println("  curl 'http://localhost:8080/users?limit=5&offset=0'")
    println("  curl http://localhost:8080/api-docs")
    println()

    // Start the server
    initialize()
  }
}
