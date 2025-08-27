# CaskChez: HTTP Framework Integration

CaskChez provides seamless integration with the Cask HTTP framework, enabling automatic request/response validation and schema introspection through annotation-driven development.

## Features

- 🌐 **Annotation-Driven APIs**: Define complete HTTP APIs with `@CaskChez.get`, `@CaskChez.post`, etc.
- 🛡️ **Automatic Request Validation**: Complete HTTP request/response validation with structured error handling
- 📋 **Schema Registry**: Centralized registry for all route schemas
- 🔍 **Schema Introspection**: Runtime access to all registered schemas and routes
- 🏷️ **OpenAPI 3.1.1**: Automatic OpenAPI specification generation
- ⚡ **Zero Boilerplate**: Eliminate manual validation code with annotations
- 🧪 **Comprehensive Validation**: Body, query parameters, path parameters, and headers

## Installation

### Mill

```scala
mvn"com.silvabyte::chez:0.2.0"
mvn"com.silvabyte::caskchez:0.2.0"
```

### SBT

```scala
libraryDependencies ++= Seq(
  "com.silvabyte" %% "chez" % "0.2.0",
  "com.silvabyte" %% "caskchez" % "0.2.0"
)
```

## Quick Start

### Annotation-Based Schema Definition

The recommended approach is to use annotations for clean, type-safe API development:

```scala
import cask.*
import caskchez.*
import chez.*
import chez.derivation.Schema
import upickle.default.*

// Define your data types with schema annotations
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
```

## CaskChez Annotations (Recommended)

### Complete CRUD API Example

```scala
object UserCrudAPI extends cask.MainRoutes {

  // In-memory storage (use real database in production)
  var users = scala.collection.mutable.Map[String, User](
    "1" -> User("1", "Alice Smith", "alice@example.com", 25, isActive = true),
    "2" -> User("2", "Bob Johnson", "bob@example.com", 32, isActive = true)
  )

  // CREATE - POST /users
  @CaskChez.post(
    "/users",
    RouteSchema(
      summary = Some("Create a new user"),
      description = Some("Creates a new user with automatic validation"),
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
            message = s"Email ${request.email} is already registered"
          )
          write(error)
        } else {
          val newId = (users.keys.map(_.toInt).maxOption.getOrElse(0) + 1).toString
          val user = User(newId, request.name, request.email, request.age, request.isActive)
          users(newId) = user
          write(user)
        }
      case Left(error) =>
        val errorResponse = ErrorResponse("validation_failed", error.message)
        write(errorResponse)
    }
  }

  // READ - GET /users
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

        // Apply filters and pagination
        val filteredUsers = users.values.filter { user =>
          val matchesSearch = query.search.isEmpty ||
            user.name.toLowerCase.contains(query.search.get.toLowerCase)
          val matchesActive = query.active.isEmpty || user.isActive == query.active.get
          matchesSearch && matchesActive
        }.toList.sortBy(_.id)

        val total = filteredUsers.length
        val totalPages = Math.max(1, Math.ceil(total.toDouble / limit).toInt)
        val startIndex = (page - 1) * limit
        val paginatedUsers = filteredUsers.slice(startIndex, startIndex + limit)

        val response = UserListResponse(paginatedUsers, total, page, limit, totalPages)
        write(response)
      case Left(error) =>
        write(ErrorResponse("validation_failed", error.message))
    }
  }

  // READ - GET /users/:id
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
      case Some(user) => write(user)
      case None => write(ErrorResponse("user_not_found", s"User with ID '$id' was not found"))
    }
  }

  // UPDATE - PUT /users/:id
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
      case None => write(ErrorResponse("user_not_found", s"User with ID '$id' was not found"))
      case Some(existingUser) =>
        validatedRequest.getBody[UpdateUserRequest] match {
          case Right(updates) =>
            // Check email conflict if email is being updated
            val emailConflict = updates.email.exists { newEmail =>
              newEmail != existingUser.email && users.values.exists(_.email == newEmail)
            }

            if (emailConflict) {
              write(ErrorResponse("email_exists", s"Email ${updates.email.get} is already registered"))
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
            write(ErrorResponse("validation_failed", error.message))
        }
    }
  }

  // DELETE - DELETE /users/:id
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
        write(SuccessResponse(s"User with ID '$id' has been deleted"))
      case None =>
        write(ErrorResponse("user_not_found", s"User with ID '$id' was not found"))
    }
  }
}
```

### GET with Path Parameters and Query Validation

```scala
// Define query parameter schema
@Schema.title("UserListQuery")
@Schema.description("Query parameters for listing users")
case class UserListQuery(
  @Schema.description("Page number for pagination")
  @Schema.minimum(1)
  page: Option[Int] = Some(1),

  @Schema.description("Number of users per page")
  @Schema.minimum(1)
  @Schema.maximum(100)
  limit: Option[Int] = Some(10),

  @Schema.description("Search term for filtering users")
  @Schema.minLength(1)
  search: Option[String] = None
) derives Schema, ReadWriter

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
      val search = query.search.getOrElse("")

      // Process with validated query parameters
      val users = findUsers(search, page, limit)
      write(UserListResponse(users, users.length, page, limit))

    case Left(error) =>
      val errorResponse = ErrorResponse(
        error = "Validation failed",
        message = error.message,
        details = List.empty
      )
      write(errorResponse)
  }
}

@CaskChez.get(
  "/users/:id",
  RouteSchema(
    summary = Some("Get user by ID"),
    description = Some("Retrieves a user by their unique ID"),
    tags = List("users"),
    responses = Map(
      200 -> ApiResponse("User found", Schema[User]),
      404 -> ApiResponse("User not found", Schema[ErrorResponse])
    )
  )
)
def getUser(id: String, validatedRequest: ValidatedRequest): cask.Response[String] = {
  findUserById(id) match {
    case Some(user) =>
      cask.Response(write(user), statusCode = 200)
    case None =>
      val error = ErrorResponse(
        error = "Not Found",
        message = s"User with ID '$id' not found",
        details = List.empty
      )
      cask.Response(write(error), statusCode = 404)
  }
}
```

### Automatic OpenAPI Generation

```scala
@CaskChez.swagger(
  "/openapi",
  OpenAPIConfig(
    title = "User Management API",
    summary = Some("Complete user management with validation"),
    description = "API demonstrating CaskChez features with JSON Schema validation",
    version = "1.0.0"
  )
)
def openapi(): String = "" // Auto-generated OpenAPI 3.1.1 specification
```

## Lower-Level Route Schema Definition

If you prefer more explicit control, you can define route schemas separately:

### Manual Route Schema Definition

```scala
// Define schema programmatically
val createUserRouteSchema = RouteSchema(
  summary = Some("Create a new user"),
  description = Some("Creates a new user with validation"),
  tags = List("users"),
  body = Some(Schema[CreateUserRequest]),
  responses = Map(
    201 -> ApiResponse("User created successfully", Schema[User]),
    400 -> ApiResponse("Validation error", Schema[ErrorResponse]),
    500 -> ApiResponse("Internal server error", Schema[ErrorResponse])
  )
)

// Use with @CaskChez annotations
@CaskChez.post("/users", createUserRouteSchema)
def createUser(validatedRequest: ValidatedRequest): cask.Response[String] = {
  // Implementation same as above
}
```

### Manual Schema Construction (Advanced)

```scala
import chez.*

// Define schemas using Chez DSL instead of annotations
val createUserRequest = Chez.Object(
  "name" -> Chez.String(minLength = Some(1), maxLength = Some(100)),
  "email" -> Chez.String(format = Some("email")),
  "age" -> Chez.Integer(minimum = Some(0), maximum = Some(150))
)

val userResponse = Chez.Object(
  "id" -> Chez.String(),
  "name" -> Chez.String(),
  "email" -> Chez.String(),
  "age" -> Chez.Integer()
)

val createUserRouteSchema = RouteSchema(
  summary = Some("Create user"),
  body = Some(createUserRequest),
  responses = Map(
    201 -> ApiResponse("Created", userResponse)
  )
)

@CaskChez.post("/users", createUserRouteSchema)
def createUser(validatedRequest: ValidatedRequest): cask.Response[String] = {
  // Manual JSON processing since no case class derivation
  validatedRequest.body match {
    case Some(json) =>
      // Process ujson.Value directly
      val name = json("name").str
      val email = json("email").str
      val age = json("age").num.toInt
      // ... create user logic
    case None =>
      cask.Response("Bad Request", statusCode = 400)
  }
}
```

### Zero Boilerplate Validation

The key benefit of CaskChez annotations is automatic validation:

```scala
@CaskChez.post("/users", createUserRouteSchema)
def createUser(validatedRequest: ValidatedRequest) = {
  // Request is already validated against schema!
  validatedRequest.getBody[CreateUserRequest] match {
    case Right(userData) => processUser(userData)
    case Left(error) => handleValidationError(error)
  }
}
```

## Schema Registry and Introspection

### Automatic Schema Registration

All routes with `@SchemaEndpoint` annotations are automatically registered in the `RouteSchemaRegistry`:

```scala
// Automatically available endpoints for schema introspection
object SchemaAPI extends cask.MainRoutes {

  @cask.get("/schemas")
  def getAllSchemas(): ujson.Value = {
    RouteSchemaRegistry.getAll
  }

  @cask.get("/schemas/:schemaId")
  def getSchema(schemaId: String): cask.Response[ujson.Value] = {
    RouteSchemaRegistry.getSchema(schemaId) match {
      case Some(schema) => cask.Response(schema)
      case None => cask.Response(ujson.Obj("error" -> "schema_not_found"), statusCode = 404)
    }
  }

  @cask.get("/routes")
  def getAllRoutes(): ujson.Value = {
    RouteSchemaRegistry.getRoutes
  }

  @cask.get("/openapi")
  def getOpenAPISpec(): ujson.Value = {
    RouteSchemaRegistry.toOpenAPI(
      info = OpenAPIInfo(
        title = "User API",
        version = "1.0.0",
        description = Some("API for user management")
      )
    )
  }
}
```

### Runtime Schema Access

```scala
// Access registered schemas at runtime
val userRoutes = RouteSchemaRegistry.getRoutesByTag("users")
val allSchemas = RouteSchemaRegistry.getAllSchemas()

// Validate data against registered schemas
val schemaId = "create-user-request"
RouteSchemaRegistry.getSchema(schemaId).foreach { schema =>
  val validationResult = schema.validate(requestData)
  if (validationResult.nonEmpty) {
    // Handle validation errors
  }
}
```

## Advanced Features

### Middleware Integration

```scala
object APIServer extends cask.MainRoutes {

  // Global middleware for all schema endpoints
  override def defaultDecorators = Seq(
    new cask.decorators.compress(),
    new cask.decorators.cors(),
    new SchemaValidationMiddleware()
  )

  // Custom validation middleware
  class SchemaValidationMiddleware extends cask.Decorator {
    def wrapFunction(ctx: cask.Request, delegate: Delegate): Result[Response.Raw] = {
      // Custom validation logic
      delegate.map { response =>
        // Process response
        response
      }
    }
  }
}
```

### Custom Error Handling

```scala
object UserAPI extends cask.MainRoutes {

  // Custom error handler for validation failures
  override def defaultHandler: cask.main.DefaultHandler.type = new cask.main.DefaultHandler {
    override def handleRouteError(routes: Routes)(ctx: cask.Request): Response.Raw = {
      ctx.exchange.getResponseHeaders.put(
        new HttpString("content-type"), "application/json"
      )

      val errorResponse = ujson.Obj(
        "error" -> "route_error",
        "message" -> "Invalid request",
        "timestamp" -> System.currentTimeMillis()
      )

      Response.Raw(
        ujson.write(errorResponse).getBytes,
        statusCode = 400
      )
    }
  }
}
```

### Security Integration

```scala
val secureRouteSchema = RouteSchema(
  summary = Some("Protected endpoint"),
  security = List(
    SecurityRequirement(
      name = "bearerAuth",
      scopes = List("read", "write")
    ),
    SecurityRequirement(
      name = "apiKey",
      scopes = List("admin")
    )
  ),
  responses = Map(
    200 -> ApiResponse("Success", dataSchema),
    401 -> ApiResponse("Unauthorized", errorResponse),
    403 -> ApiResponse("Forbidden", errorResponse)
  )
)

@SchemaEndpoint.get("/admin/users", secureRouteSchema)
def getAdminUsers(validatedRequest: ValidatedRequest): cask.Response[String] = {
  // Security validation happens automatically
  // Access user permissions from validatedRequest.security
  adminService.getAllUsers()
}
```

## OpenAPI Integration

### Generate OpenAPI Specification

```scala
val openAPISpec = RouteSchemaRegistry.toOpenAPI(
  info = OpenAPIInfo(
    title = "User Management API",
    version = "2.0.0",
    description = Some("RESTful API for user management operations"),
    contact = Some(Contact(
      name = "API Team",
      email = "api@example.com",
      url = "https://example.com/support"
    )),
    license = Some(License(
      name = "MIT",
      url = "https://opensource.org/licenses/MIT"
    ))
  ),
  servers = List(
    Server(
      url = "https://api.example.com/v2",
      description = "Production server"
    ),
    Server(
      url = "https://staging-api.example.com/v2",
      description = "Staging server"
    )
  ),
  securitySchemes = Map(
    "bearerAuth" -> SecurityScheme(
      `type` = "http",
      scheme = "bearer",
      bearerFormat = Some("JWT")
    ),
    "apiKey" -> SecurityScheme(
      `type` = "apiKey",
      name = "X-API-Key",
      in = "header"
    )
  )
)

// Serve OpenAPI spec
@cask.get("/openapi.json")
def openapi(): ujson.Value = openAPISpec
```

## Architecture

### Core Components

- **Route Schemas**: `RouteSchema`, `ApiResponse`, `Parameter`, `SecurityRequirement`
- **Custom Endpoints**: `@SchemaEndpoint.get`, `@SchemaEndpoint.post`, `@SchemaEndpoint.put`, `@SchemaEndpoint.delete`
- **Validation**: `ValidatedRequest`, `ValidatedRequestReader`, `ValidationResult`
- **Registry**: `RouteSchemaRegistry` for centralized schema management
- **Integration**: Seamless Cask HTTP framework integration
- **OpenAPI**: Automatic OpenAPI 3.0 specification generation

### Request Validation Flow

1. **Request Receipt**: Cask receives HTTP request
2. **Schema Lookup**: `@CaskChez` decorator finds registered schema
3. **Automatic Validation**: Request body, query parameters, path parameters, and headers validated against schema
4. **Processing**: If valid, request forwarded to handler with `ValidatedRequest`
5. **Response**: Response optionally validated against response schema
6. **Error Handling**: Validation errors automatically converted to structured HTTP error responses

### Validation Components

- **ValidatedRequest**: Container for validated request data with type-safe accessors
- **RouteSchema**: Schema definition for endpoint validation requirements
- **ValidationError**: Structured error types for different validation failures
- **Automatic Error Conversion**: Seamless conversion from schema validation to HTTP errors

## Examples

See the `CaskChez/src/main/scala/caskchez/examples/` package for complete HTTP integration examples:

- `UserCrudAPI.scala` - Complete user management CRUD API with validation, error handling, and OpenAPI generation

## Running Examples

```bash
# Run CaskChez User CRUD API example
./mill CaskChez.runMain caskchez.examples.UserCrudAPI

# Then test with:
curl http://localhost:8082/health
curl http://localhost:8082/users
curl -X POST http://localhost:8082/users \
  -H 'Content-Type: application/json' \
  -d '{"name": "John Doe", "email": "john@example.com", "age": 30}'
curl http://localhost:8082/openapi
```

## Testing

CaskChez includes comprehensive testing infrastructure with 43 tests across three test suites:

### Test Categories

- **Unit Tests (WebValidationTests)**: 21 tests covering HTTP validation logic
- **Integration Tests (UserCrudAPITest)**: 9 tests covering basic CRUD operations
- **Advanced Scenarios (ComprehensiveUserCrudAPITest)**: 13 tests covering edge cases, performance, and advanced validation

### Running Tests

```bash
# Run all CaskChez tests
make test-cask

# Run specific test categories
make test-web-validation    # HTTP validation unit tests
make test-integration      # Basic CRUD integration tests
make test-comprehensive    # Advanced scenario tests
```

### Test Example

```scala
// Test validated endpoints
class UserAPITest extends utest.TestSuite {

  test("create user validates request body") {
    TestServer.withServer { (host, routes) =>
      val validRequest = routes.CreateUserRequest(
        name = "John Doe",
        email = "john@example.com",
        age = 30
      )

      val response = requests.post(
        url = s"$host/users",
        data = requests.RequestBlob.ByteSourceRequestBlob(write(validRequest)),
        headers = Map("Content-Type" -> "application/json")
      )

      assert(response.statusCode == 200)
      val user = read[routes.User](response.text())
      assert(user.name == "John Doe")
    }
  }

  test("validation errors are properly structured") {
    TestServer.withServer { (host, routes) =>
      val invalidRequest = routes.CreateUserRequest(
        name = "", // Too short
        email = "invalid-email",
        age = -5
      )

      try {
        requests.post(
          url = s"$host/users",
          data = requests.RequestBlob.ByteSourceRequestBlob(write(invalidRequest)),
          headers = Map("Content-Type" -> "application/json")
        )
      } catch {
        case e: requests.RequestFailedException =>
          assert(e.response.statusCode == 400)
          val responseBody = e.response.text()
          assert(responseBody.contains("Validation failed"))
      }
    }
  }
}
```
