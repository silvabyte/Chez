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
 * User CRUD API - Complete example showcasing CaskChez best practices
 *
 * This example demonstrates:
 *   - Clean annotation-based schema derivation with @Schema.*
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
  var users = scala.collection.mutable.Map[String, User](
    "1" -> User("1", "Alice Smith", "alice@example.com", 25, isActive = true),
    "2" -> User("2", "Bob Johnson", "bob@example.com", 32, isActive = true),
    "3" -> User("3", "Carol Davis", "carol@example.com", 28, isActive = false)
  )

  // === DATA MODELS ===

  @Schema.title("User")
  @Schema.description("A user in the system")
  case class User(
    @Schema.description("Unique identifier for the user")
    @Schema.pattern("^[0-9]+$")
    id: String,

    @Schema.description("Full name of the user")
    @Schema.minLength(1)
    @Schema.maxLength(100)
    name: String,

    @Schema.description("Email address")
    @Schema.format("email")
    email: String,

    @Schema.description("Age in years")
    @Schema.minimum(0)
    @Schema.maximum(150)
    age: Int,

    @Schema.description("Whether the user account is active")
    @Schema.default(true)
    isActive: Boolean
  ) derives Schema, ReadWriter

  @Schema.title("CreateUserRequest")
  @Schema.description("Request payload for creating a new user")
  case class CreateUserRequest(
    @Schema.description("Full name of the user")
    @Schema.minLength(1)
    @Schema.maxLength(100)
    name: String,

    @Schema.description("Email address")
    @Schema.format("email")
    email: String,

    @Schema.description("Age in years")
    @Schema.minimum(0)
    @Schema.maximum(150)
    age: Int,

    @Schema.description("Whether the user account should be active")
    @Schema.default(true)
    isActive: Boolean = true
  ) derives Schema, ReadWriter

  @Schema.title("UpdateUserRequest")
  @Schema.description("Request payload for updating an existing user")
  case class UpdateUserRequest(
    @Schema.description("Full name of the user")
    @Schema.minLength(1)
    @Schema.maxLength(100)
    name: Option[String] = None,

    @Schema.description("Email address")
    @Schema.format("email")
    email: Option[String] = None,

    @Schema.description("Age in years")
    @Schema.minimum(0)
    @Schema.maximum(150)
    age: Option[Int] = None,

    @Schema.description("Whether the user account is active")
    isActive: Option[Boolean] = None
  ) derives Schema, ReadWriter

  @Schema.title("UserListQuery")
  @Schema.description("Query parameters for filtering and paginating users")
  case class UserListQuery(
    @Schema.description("Page number (1-based)")
    @Schema.minimum(1)
    @Schema.default(1)
    page: Option[Int] = Some(1),

    @Schema.description("Number of users per page")
    @Schema.minimum(1)
    @Schema.maximum(100)
    @Schema.default(10)
    limit: Option[Int] = Some(10),

    @Schema.description("Filter by name (case-insensitive partial match)")
    @Schema.minLength(1)
    search: Option[String] = None,

    @Schema.description("Filter by active status")
    active: Option[Boolean] = None
  ) derives Schema, ReadWriter

  @Schema.title("UserListResponse")
  @Schema.description("Paginated list of users with metadata")
  case class UserListResponse(
    @Schema.description("List of users for this page")
    users: List[User],

    @Schema.description("Total number of users matching filters")
    total: Int,

    @Schema.description("Current page number")
    page: Int,

    @Schema.description("Number of users per page")
    limit: Int,

    @Schema.description("Total number of pages")
    totalPages: Int
  ) derives Schema, ReadWriter

  @Schema.title("ErrorResponse")
  @Schema.description("Error response with details")
  case class ErrorResponse(
    @Schema.description("Error type identifier")
    error: String,

    @Schema.description("Human-readable error message")
    message: String,

    @Schema.description("Additional error details")
    details: List[String] = List.empty
  ) derives Schema, ReadWriter

  @Schema.title("SuccessResponse")
  @Schema.description("Generic success response")
  case class SuccessResponse(
    @Schema.description("Success message")
    message: String
  ) derives Schema, ReadWriter

  // === CRUD ENDPOINTS ===

  @CaskChez.post(
    "/users",
    RouteSchema(
      summary = Some("Create a new user"),
      description = Some("Creates a new user with automatic validation and returns the created user"),
      tags = List("users"),
      body = Some(Schema[CreateUserRequest]),
      responses = Map(
        201 -> ApiResponse("User created successfully", Schema[User]),
        400 -> ApiResponse("Validation error", Schema[ErrorResponse]),
        409 -> ApiResponse("Email already exists", Schema[ErrorResponse])
      )
    )
  )
  def createUser(validatedRequest: ValidatedRequest): String = {
    validatedRequest.getBody[CreateUserRequest] match {
      case Right(request) =>
        // Check if email already exists
        if (users.values.exists(_.email == request.email)) {
          val error = ErrorResponse(
            error = "email_exists",
            message = s"Email ${request.email} is already registered",
            details = List("Please use a different email address")
          )
          write(error)
        } else {
          val newId = (users.keys.map(_.toInt).maxOption.getOrElse(0) + 1).toString
          val user = User(
            id = newId,
            name = request.name,
            email = request.email,
            age = request.age,
            isActive = request.isActive
          )
          users(newId) = user
          write(user)
        }

      case Left(error) =>
        val errorResponse = ErrorResponse(
          error = "validation_failed",
          message = error.message,
          details = List("Please check your request data")
        )
        write(errorResponse)
    }
  }

  @CaskChez.get(
    "/users",
    RouteSchema(
      summary = Some("List users"),
      description = Some("Retrieves a paginated list of users with optional filtering"),
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
        val search = query.search.getOrElse("")
        val activeFilter = query.active

        // Apply filters
        val filteredUsers = users.values.filter { user =>
          val matchesSearch = search.isEmpty || user.name.toLowerCase.contains(search.toLowerCase)
          val matchesActive = activeFilter.isEmpty || user.isActive == activeFilter.get
          matchesSearch && matchesActive
        }.toList.sortBy(_.id)

        // Apply pagination
        val total = filteredUsers.length
        val totalPages = Math.max(1, Math.ceil(total.toDouble / limit).toInt)
        val startIndex = (page - 1) * limit
        val endIndex = Math.min(startIndex + limit, total)
        val paginatedUsers = if (startIndex < total) filteredUsers.slice(startIndex, endIndex) else List.empty

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

  @CaskChez.get(
    "/users/:id",
    RouteSchema(
      summary = Some("Get user by ID"),
      description = Some("Retrieves a specific user by their unique identifier"),
      tags = List("users"),
      responses = Map(
        200 -> ApiResponse("User found", Schema[User]),
        404 -> ApiResponse("User not found", Schema[ErrorResponse])
      )
    )
  )
  def getUser(id: String, validatedRequest: ValidatedRequest): String = {
    users.get(id) match {
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

  @CaskChez.put(
    "/users/:id",
    RouteSchema(
      summary = Some("Update user"),
      description = Some("Updates an existing user with partial data"),
      tags = List("users"),
      body = Some(Schema[UpdateUserRequest]),
      responses = Map(
        200 -> ApiResponse("User updated successfully", Schema[User]),
        400 -> ApiResponse("Validation error", Schema[ErrorResponse]),
        404 -> ApiResponse("User not found", Schema[ErrorResponse]),
        409 -> ApiResponse("Email already exists", Schema[ErrorResponse])
      )
    )
  )
  def updateUser(id: String, validatedRequest: ValidatedRequest): String = {
    users.get(id) match {
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
              newEmail != existingUser.email && users.values.exists(_.email == newEmail)
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
              users(id) = updatedUser
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

  @CaskChez.delete(
    "/users/:id",
    RouteSchema(
      summary = Some("Delete user"),
      description = Some("Deletes a user by their unique identifier"),
      tags = List("users"),
      responses = Map(
        200 -> ApiResponse("User deleted successfully", Schema[SuccessResponse]),
        404 -> ApiResponse("User not found", Schema[ErrorResponse])
      )
    )
  )
  def deleteUser(id: String, validatedRequest: ValidatedRequest): String = {
    users.get(id) match {
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

  @CaskChez.get(
    "/health",
    RouteSchema(
      summary = Some("Health check"),
      description = Some("Returns the health status of the API service"),
      tags = List("system"),
      responses = Map(
        200 -> ApiResponse("Service is healthy", Schema[SuccessResponse])
      )
    )
  )
  def health(): String = {
    val response = SuccessResponse("User CRUD API is healthy and ready")
    write(response)
  }

  @CaskChez.swagger(
    "/openapi",
    OpenAPIConfig(
      title = "User CRUD API",
      summary = Some("Complete user management API with validation"),
      description = "RESTful API for user management built with CaskChez, featuring automatic validation, comprehensive error handling, and OpenAPI documentation",
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
  println("✨ CaskChez features demonstrated:")
  println("  • Annotation-based schema derivation (@Schema.*)")
  println("  • Automatic request/response validation")
  println("  • Type-safe query parameters and path variables")
  println("  • Comprehensive error handling with proper HTTP status codes")
  println("  • OpenAPI 3.1.1 specification generation")
  println("  • Clean REST API design patterns")
  println()
  println(s"🌐 Server running at http://${host}:${port}")

  initialize()
}