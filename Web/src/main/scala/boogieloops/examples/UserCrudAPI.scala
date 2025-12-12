package boogieloops.web.examples

import cask._
import _root_.boogieloops.schema._
import boogieloops.web._
import boogieloops.web.Web
import boogieloops.web.Web.ValidatedRequestReader
import boogieloops.web.openapi.config.OpenAPIConfig
import upickle.default._
import boogieloops.schema.derivation.Schematic
import scala.jdk.CollectionConverters._

/**
 * User CRUD API - Complete example showcasing boogieloops.web best practices
 *
 * This example demonstrates:
 *   - Clean annotation-based schema derivation with @Schematic.*
 *   - Full CRUD operations (Create, Read, Update, Delete)
 *   - Automatic request/response validation
 *   - Type-safe query parameters and path variables
 *   - Comprehensive error handling
 *   - OpenAPI 3.1.1 specification generation
 *   - Best practices for REST API design
 */
object UserCrudAPI extends cask.MainRoutes {

  override def port = 8082
  override def host = "0.0.0.0"

  // In-memory storage (use real database in production)
  // Using a thread-safe mutable map for the example
  private val users = new java.util.concurrent.ConcurrentHashMap[String, User]()

  // Initialize with sample data
  {
    users.put("1", User("1", "Alice Smith", "alice@example.com", 25, isActive = true))
    users.put("2", User("2", "Bob Johnson", "bob@example.com", 32, isActive = true))
    users.put("3", User("3", "Carol Davis", "carol@example.com", 28, isActive = false))
  }

  // === DATA MODELS ===

  @Schematic.title("User")
  @Schematic.description("A user in the system")
  case class User(
      @Schematic.description("Unique identifier for the user")
      @Schematic.pattern("^[0-9]+$")
      id: String,
      @Schematic.description("Full name of the user")
      @Schematic.minLength(1)
      @Schematic.maxLength(100)
      name: String,
      @Schematic.description("Email address")
      @Schematic.format("email")
      email: String,
      @Schematic.description("Age in years")
      @Schematic.minimum(0)
      @Schematic.maximum(150)
      age: Int,
      @Schematic.description("Whether the user account is active")
      @Schematic.default(true)
      isActive: Boolean
  ) derives Schematic, ReadWriter

  @Schematic.title("CreateUserRequest")
  @Schematic.description("Request payload for creating a new user")
  case class CreateUserRequest(
      @Schematic.description("Full name of the user")
      @Schematic.minLength(1)
      @Schematic.maxLength(100)
      name: String,
      @Schematic.description("Email address")
      @Schematic.format("email")
      email: String,
      @Schematic.description("Age in years")
      @Schematic.minimum(0)
      @Schematic.maximum(150)
      age: Int,
      @Schematic.description("Whether the user account should be active")
      @Schematic.default(true)
      isActive: Boolean = true
  ) derives Schematic, ReadWriter

  @Schematic.title("UpdateUserRequest")
  @Schematic.description("Request payload for updating an existing user")
  case class UpdateUserRequest(
      @Schematic.description("Full name of the user")
      @Schematic.minLength(1)
      @Schematic.maxLength(100)
      name: Option[String] = None,
      @Schematic.description("Email address")
      @Schematic.format("email")
      email: Option[String] = None,
      @Schematic.description("Age in years")
      @Schematic.minimum(0)
      @Schematic.maximum(150)
      age: Option[Int] = None,
      @Schematic.description("Whether the user account is active")
      isActive: Option[Boolean] = None
  ) derives Schematic, ReadWriter

  @Schematic.title("UserListQuery")
  @Schematic.description("Query parameters for filtering and paginating users")
  case class UserListQuery(
      @Schematic.description("Page number (1-based)")
      @Schematic.minimum(1)
      @Schematic.default(1)
      page: Option[Int] = Some(1),
      @Schematic.description("Number of users per page")
      @Schematic.minimum(1)
      @Schematic.maximum(100)
      @Schematic.default(10)
      limit: Option[Int] = Some(10),
      @Schematic.description("Filter by name (case-insensitive partial match)")
      @Schematic.minLength(1)
      search: Option[String] = None,
      @Schematic.description("Filter by active status")
      active: Option[Boolean] = None
  ) derives Schematic, ReadWriter

  @Schematic.title("UserListResponse")
  @Schematic.description("Paginated list of users with metadata")
  case class UserListResponse(
      @Schematic.description("List of users for this page")
      users: List[User],
      @Schematic.description("Total number of users matching filters")
      total: Int,
      @Schematic.description("Current page number")
      page: Int,
      @Schematic.description("Number of users per page")
      limit: Int,
      @Schematic.description("Total number of pages")
      totalPages: Int
  ) derives Schematic, ReadWriter

  @Schematic.title("ErrorResponse")
  @Schematic.description("Error response with details")
  case class ErrorResponse(
      @Schematic.description("Error type identifier")
      error: String,
      @Schematic.description("Human-readable error message")
      message: String,
      @Schematic.description("Additional error details")
      details: List[String] = List.empty
  ) derives Schematic, ReadWriter

  @Schematic.title("SuccessResponse")
  @Schematic.description("Generic success response")
  case class SuccessResponse(
      @Schematic.description("Success message")
      message: String
  ) derives Schematic, ReadWriter

  object ExampleData {
    val user1 = User("1", "Alice Smith", "alice@example.com", 25, isActive = true)
    val createUser = CreateUserRequest("John Doe", "john@example.com", 30, isActive = true)
    val updateUser = UpdateUserRequest(name = Some("Jane Doe"), email = None, age = Some(31), isActive = None)
    val listQuery = UserListQuery(page = Some(1), limit = Some(10), search = Some("ali"), active = Some(true))
    val validationError = ErrorResponse(
      error = "validation_failed",
      message = "Email already exists",
      details = List("Use a different email")
    )
    val success = SuccessResponse(message = "User deleted successfully")
  }

  // === CRUD ENDPOINTS ===

  @Web.post(
    "/users",
    RouteSchema(
      summary = Some("Create a new user"),
      description =
        Some("Creates a new user with automatic validation and returns the created user"),
      tags = List("users"),
      body = Some(Schematic[CreateUserRequest].withExamples(ujson.read(write(ExampleData.createUser)))),
      responses = Map(
        201 -> ApiResponse(
          "User created successfully",
          Schematic[User].withExamples(ujson.read(write(ExampleData.user1)))
        ),
        400 -> ApiResponse(
          "Validation error",
          Schematic[ErrorResponse].withExamples(ujson.read(write(ExampleData.validationError)))
        ),
        409 -> ApiResponse(
          "Email already exists",
          Schematic[ErrorResponse].withExamples(ujson.read(write(ExampleData.validationError)))
        )
      )
    )
  )
  def createUser(validatedRequest: ValidatedRequest): cask.Response[String] = {
    validatedRequest.getBody[CreateUserRequest] match {
      case Right(request) =>
        // Check if email already exists
        import scala.jdk.CollectionConverters._
        if (users.values.asScala.exists(_.email == request.email)) {
          val error = ErrorResponse(
            error = "email_exists",
            message = s"Email ${request.email} is already registered",
            details = List("Please use a different email address")
          )
          cask.Response(
            data = write(error),
            statusCode = 409,
            headers = Seq("Content-Type" -> "application/json")
          )
        } else {
          val newId = (users.keySet.asScala.map(_.toInt).maxOption.getOrElse(0) + 1).toString
          val user = User(
            id = newId,
            name = request.name,
            email = request.email,
            age = request.age,
            isActive = request.isActive
          )
          users.put(newId, user)
          cask.Response(
            data = write(user),
            statusCode = 201,
            headers = Seq("Content-Type" -> "application/json")
          )
        }

      case Left(error) =>
        val errorResponse = ErrorResponse(
          error = "validation_failed",
          message = error.message,
          details = List("Please check your request data")
        )
        cask.Response(
          data = write(errorResponse),
          statusCode = 400,
          headers = Seq("Content-Type" -> "application/json")
        )
    }
  }

  @Web.get(
    "/users",
    RouteSchema(
      summary = Some("List users"),
      description = Some("Retrieves a paginated list of users with optional filtering"),
      tags = List("users"),
      query = Some(Schematic[UserListQuery].withExamples(ujson.read(write(ExampleData.listQuery)))),
      responses = Map(
        200 -> ApiResponse(
          "Users retrieved successfully",
          Schematic[UserListResponse].withExamples(ujson.read(write(ExampleData.success)))
        )
      )
    )
  )
  def listUsers(validatedRequest: ValidatedRequest): String = {
    validatedRequest.getQuery[UserListQuery] match {
      case Right(query) =>
        val page = query.page.getOrElse(1)
        val limit = query.limit.getOrElse(10)
        val search = query.search.getOrElse("")
        val activeFilter = query.active

        // Apply filters
        val filteredUsers = users.values.asScala.filter { user =>
          val matchesSearch = search.isEmpty || user.name.toLowerCase.contains(search.toLowerCase)
          val matchesActive = activeFilter.isEmpty || user.isActive == activeFilter.get
          matchesSearch && matchesActive
        }.toList.sortBy(_.id)

        // Apply pagination
        val total = filteredUsers.length
        val totalPages = Math.max(1, Math.ceil(total.toDouble / limit).toInt)
        val startIndex = (page - 1) * limit
        val endIndex = Math.min(startIndex + limit, total)
        val paginatedUsers =
          if (startIndex < total) filteredUsers.slice(startIndex, endIndex) else List.empty

        val response = UserListResponse(
          users = paginatedUsers,
          total = total,
          page = page,
          limit = limit,
          totalPages = totalPages
        )
        write(response)

      case Left(error) =>
        val errorResponse = ErrorResponse(
          error = "validation_failed",
          message = error.message
        )
        write(errorResponse)
    }
  }

  @Web.get(
    "/users/:id",
    RouteSchema(
      summary = Some("Get user by ID"),
      description = Some("Retrieves a specific user by their unique identifier"),
      tags = List("users"),
      responses = Map(
        200 -> ApiResponse(
          "User found",
          Schematic[User].withExamples(ujson.read(write(ExampleData.user1)))
        ),
        404 -> ApiResponse(
          "User not found",
          Schematic[ErrorResponse].withExamples(ujson.read(write(ExampleData.validationError)))
        )
      )
    )
  )
  def getUser(id: String, validatedRequest: ValidatedRequest): String = {
    val _ = validatedRequest // Suppress unused warning
    val userOpt: Option[User] = Option(users.get(id))
    userOpt match {
      case Some(user) =>
        write(user)
      case None =>
        val error = ErrorResponse(
          error = "user_not_found",
          message = s"User with ID '$id' was not found"
        )
        write(error)
    }
  }

  @Web.put(
    "/users/:id",
    RouteSchema(
      summary = Some("Update user"),
      description = Some("Updates an existing user with partial data"),
      tags = List("users"),
      body = Some(Schematic[UpdateUserRequest].withExamples(ujson.read(write(ExampleData.updateUser)))),
      responses = Map(
        200 -> ApiResponse(
          "User updated successfully",
          Schematic[User].withExamples(ujson.read(write(ExampleData.user1)))
        ),
        400 -> ApiResponse(
          "Validation error",
          Schematic[ErrorResponse].withExamples(ujson.read(write(ExampleData.validationError)))
        ),
        404 -> ApiResponse(
          "User not found",
          Schematic[ErrorResponse].withExamples(ujson.read(write(ExampleData.validationError)))
        ),
        409 -> ApiResponse(
          "Email already exists",
          Schematic[ErrorResponse].withExamples(ujson.read(write(ExampleData.validationError)))
        )
      )
    )
  )
  def updateUser(id: String, validatedRequest: ValidatedRequest): String = {
    val userOpt: Option[User] = Option(users.get(id))
    userOpt match {
      case None =>
        val error = ErrorResponse(
          error = "user_not_found",
          message = s"User with ID '$id' was not found"
        )
        write(error)

      case Some(existingUser) =>
        validatedRequest.getBody[UpdateUserRequest] match {
          case Right(updates) =>
            // Check email conflict if email is being updated
            val emailConflict = updates.email.exists { newEmail =>
              newEmail != existingUser.email && users.values.asScala.exists(_.email == newEmail)
            }

            if (emailConflict) {
              val error = ErrorResponse(
                error = "email_exists",
                message = s"Email ${updates.email.get} is already registered",
                details = List("Please use a different email address")
              )
              write(error)
            } else {
              val updatedUser = existingUser.copy(
                name = updates.name.getOrElse(existingUser.name),
                email = updates.email.getOrElse(existingUser.email),
                age = updates.age.getOrElse(existingUser.age),
                isActive = updates.isActive.getOrElse(existingUser.isActive)
              )
              users.put(id, updatedUser)
              write(updatedUser)
            }

          case Left(error) =>
            val errorResponse = ErrorResponse(
              error = "validation_failed",
              message = error.message
            )
            write(errorResponse)
        }
    }
  }

  @Web.delete(
    "/users/:id",
    RouteSchema(
      summary = Some("Delete user"),
      description = Some("Deletes a user by their unique identifier"),
      tags = List("users"),
      responses = Map(
        200 -> ApiResponse(
          "User deleted successfully",
          Schematic[SuccessResponse].withExamples(ujson.read(write(ExampleData.success)))
        ),
        404 -> ApiResponse(
          "User not found",
          Schematic[ErrorResponse].withExamples(ujson.read(write(ExampleData.validationError)))
        )
      )
    )
  )
  def deleteUser(id: String, validatedRequest: ValidatedRequest): String = {
    val _ = validatedRequest // Suppress unused warning
    val userOpt: Option[User] = Option(users.get(id))
    userOpt match {
      case Some(_) =>
        users.remove(id)
        val response = SuccessResponse(s"User with ID '$id' has been deleted")
        write(response)
      case None =>
        val error = ErrorResponse(
          error = "user_not_found",
          message = s"User with ID '$id' was not found"
        )
        write(error)
    }
  }

  // === UTILITY ENDPOINTS ===

  @Web.get(
    "/health",
    RouteSchema(
      summary = Some("Health check"),
      description = Some("Returns the health status of the API service"),
      tags = List("system"),
      responses = Map(
        200 -> ApiResponse("Service is healthy", Schematic[SuccessResponse])
      )
    )
  )
  def health(): String = {
    val response = SuccessResponse("User CRUD API is healthy and ready")
    write(response)
  }

  @Web.swagger(
    "/openapi",
    OpenAPIConfig(
      title = "User CRUD API",
      summary = Some("Complete user management API with validation"),
      description =
        "RESTful API for user management built with boogieloops.web, featuring automatic validation, comprehensive error handling, and OpenAPI documentation",
      version = "1.0.0"
    )
  )
  def openapi(): String = "" // Auto-generated OpenAPI 3.1.1 specification

  // Schema introspection endpoints
  @cask.get("/debug/schemas")
  def getSchemas(): ujson.Value = {
    val routes = RouteSchemaRegistry.getAll
    val schemasList = routes.map { case (key, schema) =>
      key -> ujson.Obj(
        "summary" -> schema.summary.getOrElse(""),
        "description" -> schema.description.getOrElse(""),
        "tags" -> ujson.Arr(schema.tags.map(ujson.Str.apply)*),
        "hasBody" -> schema.body.isDefined,
        "hasQuery" -> schema.query.isDefined,
        "responseCount" -> schema.responses.size
      )
    }
    ujson.Obj(
      "schemas" -> ujson.Obj.from(schemasList),
      "total" -> routes.size
    )
  }

  // Startup message
  println("🚀 User CRUD API Server Starting...")
  println("📊 Available endpoints:")
  println("  GET     /health           - Health check")
  println("  GET     /users            - List users (with filtering & pagination)")
  println("  POST    /users            - Create new user")
  println("  GET     /users/{id}       - Get user by ID")
  println("  PUT     /users/{id}       - Update user")
  println("  DELETE  /users/{id}       - Delete user")
  println("  GET     /openapi          - OpenAPI 3.1.1 specification")
  println("  GET     /debug/schemas    - Schema introspection")
  println()
  println("🔧 Quick test commands:")
  println(s"  curl http://${host}:${port}/health")
  println(s"  curl http://${host}:${port}/users")
  println(s"""  curl -X POST http://${host}:${port}/users \\""")
  println("""    -H 'Content-Type: application/json' \\""")
  println("""    -d '{"name": "John Doe", "email": "john@example.com", "age": 30}'""")
  println(s"  curl http://${host}:${port}/users/1")
  println(s"""  curl -X PUT http://${host}:${port}/users/1 \\""")
  println("""    -H 'Content-Type: application/json' \\""")
  println("""    -d '{"age": 31}'""")
  println(s"  curl -X DELETE http://${host}:${port}/users/1")
  println()
  println("✨ boogieloops.web features demonstrated:")
  println("  • Annotation-based schema derivation (@Schematic.*)")
  println("  • Automatic request/response validation")
  println("  • Type-safe query parameters and path variables")
  println("  • Comprehensive error handling with proper HTTP status codes")
  println("  • OpenAPI 3.1.1 specification generation")
  println("  • Clean REST API design patterns")
  println()
  println(s"🌐 Server running at http://${host}:${port}")

  initialize()
}
