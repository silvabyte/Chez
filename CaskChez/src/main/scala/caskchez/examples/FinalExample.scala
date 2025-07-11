package caskchez.examples

import cask._
import _root_.chez._
import caskchez._
import caskchez.CaskChez
import caskchez.CaskChez.ValidatedRequestReader
import caskchez.openapi.config.OpenAPIConfig
import upickle.default._
import chez.derivation.Schema

/**
 * Final example demonstrating CaskChez with Schema annotations and type class derivation
 *
 * This example showcases:
 *   - Schema annotations (@Schema.title, @Schema.description, etc.)
 *   - Type class derivation (derives Schema, ReadWriter)
 *   - Complete request/response validation
 *   - Automatic OpenAPI 3.1.1 generation
 *   - Path parameters, query parameters, and request bodies
 *   - TypeScript SDK generation compatibility
 */
object FinalExample extends cask.MainRoutes {

  override def port = 8082
  override def host = "0.0.0.0"
  var mockUsers = Vector(
    User(java.util.UUID.randomUUID().toString, "Alice Smith", "alice@example.com", 25),
    User(java.util.UUID.randomUUID().toString, "Bob Johnson", "bob@example.com", 32),
    User(java.util.UUID.randomUUID().toString, "Carol Davis", "carol@example.com", 28)
  )

  // Data types
  @Schema.title("CreateUserRequest")
  @Schema.description("Request to create a new user")
  case class CreateUserRequest(
      @Schema.title("Name")
      @Schema.description("The name of the user")
      @Schema.minLength(1)
      @Schema.maxLength(100)
      name: String,
      @Schema.title("Email")
      @Schema.description("The email of the user")
      @Schema.format("email")
      email: String,
      @Schema.title("Age")
      @Schema.description("The age of the user")
      @Schema.minimum(0)
      @Schema.maximum(150)
      age: Int
  ) derives Schema,
        ReadWriter

  @Schema.title("User")
  @Schema.description("A user")
  case class User(
      @Schema.title("ID")
      @Schema.description("The ID of the user")
      id: String,
      @Schema.title("Name")
      @Schema.description("The name of the user")
      @Schema.minLength(1)
      @Schema.maxLength(100)
      name: String,
      @Schema.title("Email")
      @Schema.description("The email of the user")
      @Schema.format("email")
      email: String,
      @Schema.title("Age")
      @Schema.description("The age of the user")
      @Schema.minimum(0)
      @Schema.maximum(150)
      age: Int
  ) derives Schema,
        ReadWriter

  @Schema.title("ErrorResponse")
  @Schema.description("An error response")
  case class ErrorResponse(
      @Schema.title("Error")
      @Schema.description("The error message")
      error: String,
      @Schema.title("Message")
      @Schema.description("The error message")
      message: String,
      @Schema.title("Details")
      @Schema.description("The error details")
      details: List[String]
  ) derives Schema,
        ReadWriter

  // Schema definitions
  val createUserBodySchema = Schema[CreateUserRequest]

  val userResponseSchema = Schema[User]

  val errorResponseSchema = Schema[ErrorResponse]

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

  // Hello response type
  @Schema.title("HelloResponse")
  @Schema.description("Simple hello message")
  case class HelloResponse(
      @Schema.title("Message")
      @Schema.description("The hello message")
      message: String
  ) derives Schema,
        ReadWriter

  @CaskChez.get(
    "/",
    RouteSchema(
      summary = Some("Hello endpoint"),
      description = Some("Returns a simple hello message"),
      tags = List("general"),
      responses = Map(
        200 -> ApiResponse("Hello message", Schema[HelloResponse])
      )
    )
  )
  def hello(): String = {
    val response = HelloResponse("Hello from Final CaskChez Example!")
    write(response)
  }

  // Health response type
  @Schema.title("HealthResponse")
  @Schema.description("Health check response")
  case class HealthResponse(
      @Schema.title("Status")
      @Schema.description("Health status")
      status: String,
      @Schema.title("Timestamp")
      @Schema.description("Current timestamp")
      timestamp: Long,
      @Schema.title("Service")
      @Schema.description("Service name")
      service: String,
      @Schema.title("Approach")
      @Schema.description("Implementation approach")
      approach: String
  ) derives Schema,
        ReadWriter

  @CaskChez.get(
    "/health",
    RouteSchema(
      summary = Some("Health check"),
      description = Some("Returns the health status of the service"),
      tags = List("health"),
      responses = Map(
        200 -> ApiResponse("Service is healthy", Schema[HealthResponse])
      )
    )
  )
  def health(): String = {
    val response = HealthResponse(
      status = "healthy",
      timestamp = System.currentTimeMillis(),
      service = "Final CaskChez Example",
      approach = "Custom endpoints with automatic validation"
    )
    write(response)
  }

  @CaskChez.post("/users", createUserRouteSchema)
  def createUser(validatedRequest: ValidatedRequest): cask.Response[String] = {
    validatedRequest.getBody[CreateUserRequest] match {
      case Right(userData) =>
        val user = User(
          id = java.util.UUID.randomUUID().toString,
          name = userData.name,
          email = userData.email,
          age = userData.age
        )
        mockUsers = mockUsers :+ user
        cask.Response(
          write(user),
          statusCode = 201,
          headers = Seq("Content-Type" -> "application/json")
        )
      case Left(error) =>
        val errorResponse = ErrorResponse(
          error = "Validation failed",
          message = error.message,
          details = List.empty
        )
        cask.Response(
          write(errorResponse),
          statusCode = 400,
          headers = Seq("Content-Type" -> "application/json")
        )
    }
  }

  // OpenAPI 3.1.1 specification endpoint
  @CaskChez.swagger(
    "/openapi",
    OpenAPIConfig(
      title = "CaskChez Example API",
      summary = Some("Demonstration of CaskChez with automatic OpenAPI generation"),
      description = "Complete example showcasing CaskChez features with JSON Schema 2020-12 validation",
      version = "1.0.0"
    )
  )
  def openapi(): String = "" // Auto-generated OpenAPI 3.1.1 specification

  // User lookup by ID
  @Schema.title("UserQuery")
  @Schema.description("Query parameters for user lookup")
  case class UserQuery(
      @Schema.title("Include Details")
      @Schema.description("Whether to include full user details")
      includeDetails: Option[Boolean] = Some(false)
  ) derives Schema,
        ReadWriter

  @CaskChez.get(
    "/users/:id",
    RouteSchema(
      summary = Some("Get user by ID"),
      description = Some("Retrieves a user by their unique ID"),
      tags = List("users"),
      params = Some(Schema[UserQuery]),
      responses = Map(
        200 -> ApiResponse("User found", Schema[User]),
        404 -> ApiResponse("User not found", Schema[ErrorResponse])
      )
    )
  )
  def getUser(
      id: String,
      validatedRequest: ValidatedRequest
  ): cask.Response[String] = {
    val user = mockUsers.find(_.id == id)
    user match {
      case Some(user) =>
        cask.Response(
          write(user),
          statusCode = 200,
          headers = Seq("Content-Type" -> "application/json")
        )
      case None =>
        val error = ErrorResponse(
          error = "Not Found",
          message = s"User with ID '$id' not found",
          details = List.empty
        )
        cask.Response(
          write(error),
          statusCode = 404,
          headers = Seq("Content-Type" -> "application/json")
        )
    }

  }

  // User list with query parameters
  @Schema.title("UserListQuery")
  @Schema.description("Query parameters for listing users")
  case class UserListQuery(
      @Schema.title("Page")
      @Schema.description("Page number for pagination")
      @Schema.minimum(1)
      page: Option[Int] = Some(1),
      @Schema.title("Limit")
      @Schema.description("Number of users per page")
      @Schema.minimum(1)
      @Schema.maximum(100)
      limit: Option[Int] = Some(10),
      @Schema.title("Search")
      @Schema.description("Search term for filtering users")
      @Schema.minLength(1)
      search: Option[String] = None
  ) derives Schema,
        ReadWriter

  @Schema.title("UserListResponse")
  @Schema.description("Paginated list of users")
  case class UserListResponse(
      @Schema.title("Users")
      @Schema.description("List of users")
      users: List[User],
      @Schema.title("Total")
      @Schema.description("Total number of users")
      total: Int,
      @Schema.title("Page")
      @Schema.description("Current page number")
      page: Int,
      @Schema.title("Limit")
      @Schema.description("Users per page")
      limit: Int
  ) derives Schema,
        ReadWriter

  @CaskChez.get(
    "/users",
    RouteSchema(
      summary = Some("List users"),
      description = Some("Retrieves a paginated list of users with optional search"),
      tags = List("users"),
      query = Some(Schema[UserListQuery]),
      responses = Map(
        200 -> ApiResponse("Users retrieved successfully", Schema[UserListResponse])
      )
    )
  )
  def listUsers(validatedRequest: ValidatedRequest): String = {

    validatedRequest.getQuery[UserListQuery] match {
      case Right(query) =>
        val page = query.page.getOrElse(1)
        val limit = query.limit.getOrElse(10)
        val search = query.search.getOrElse("") // TODO: use a real search
        val users = mockUsers.filter(user => user.name.contains(search))
        val total = users.length
        val paginatedUsers = users.slice((page - 1) * limit, page * limit)
        val response = UserListResponse(
          users = paginatedUsers.toList,
          total = total,
          page = page,
          limit = limit
        )
        write(response)
      case Left(error) =>
        val errorResponse = ErrorResponse(
          error = "Validation failed",
          message = error.message,
          details = List.empty
        )
        write(errorResponse)
    }
  }

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
  println("  GET  /health              - Health check with typed response")
  println("  GET  /users               - List users with query parameters")
  println("  POST /users               - Create user with full validation")
  println("  GET  /users/{id}          - Get user by ID with path parameters")
  println("  GET  /openapi             - OpenAPI 3.1.1 specification")
  println("  GET  /schemas             - View complete route schema definitions")
  println("  GET  /routes              - View all registered routes from RouteSchemaRegistry")
  println()
  println("🔧 Test commands:")
  println("  # Test basic endpoints")
  println(s"  curl http://${host}:${port}/")
  println(s"  curl http://${host}:${port}/health")
  println()
  println("  # Test user creation with validation")
  println(s"""  curl -X POST http://${host}:${port}/users \\""")
  println("""    -H 'Content-Type: application/json' \\""")
  println("""    -d '{"name": "John Doe", "email": "john@example.com", "age": 30}'""")
  println()
  println("  # Test user list with query parameters")
  println(s"  curl http://${host}:${port}/users")
  println(s"  curl http://${host}:${port}/users?page=1&limit=5&search=alice")
  println()
  println("  # Test user retrieval")
  println(s"  curl http://${host}:${port}/users/user_123")
  println()
  println("  # View OpenAPI spec")
  println(s"  curl http://${host}:${port}/openapi | jq")
  println()
  println(s"✅ Server started on http://${host}:${port}")
  println("🎯 Features demonstrated:")
  println("  • Schema annotations with @Schema.title, @Schema.description, etc.")
  println("  • Type class derivation with 'derives Schema, ReadWriter'")
  println("  • Automatic OpenAPI 3.1.1 generation")
  println("  • Full request/response validation")
  println("  • TypeScript SDK generation ready")

  initialize()
}
