package caskchez

import cask._
import _root_.chez._
import caskchez._
import caskchez.CaskChez
import caskchez.CaskChez.ValidatedRequestReader
import upickle.default._
import chez.derivation.Schema

/**
 * Test version of UserCrudAPI for integration testing
 * 
 * This is a simplified version of the main UserCrudAPI specifically designed for testing
 * with in-memory storage that can be easily reset between tests.
 */
class TestUserCrudAPI extends cask.MainRoutes {

  // In-memory storage that can be reset for tests
  var users = scala.collection.mutable.Map[String, User](
    "1" -> User("1", "Alice Smith", "alice@example.com", 25, isActive = true),
    "2" -> User("2", "Bob Johnson", "bob@example.com", 32, isActive = true)
  )

  // Reset storage for clean tests
  def resetUsers(): Unit = {
    users.clear()
    users += "1" -> User("1", "Alice Smith", "alice@example.com", 25, isActive = true)
    users += "2" -> User("2", "Bob Johnson", "bob@example.com", 32, isActive = true)
  }

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

  // === CRUD ENDPOINTS ===

  @CaskChez.post(
    "/users",
    RouteSchema(
      summary = Some("Create a new user"),
      description = Some("Creates a new user with automatic validation"),
      tags = List("users"),
      body = Some(Schema[CreateUserRequest]),
      responses = Map(
        201 -> ApiResponse("User created successfully", Schema[User]),
        400 -> ApiResponse("Validation error", Schema[ErrorResponse])
      )
    )
  )
  def createUser(validatedRequest: ValidatedRequest): String = {
    validatedRequest.getBody[CreateUserRequest] match {
      case Right(userFromBody) =>
        // Check if email already exists
        if (users.values.exists(_.email == userFromBody.email)) {
          val error = ErrorResponse(
            error = "email_exists",
            message = s"Email ${userFromBody.email} is already registered"
          )
          write(error)
        } else {
          val newId = (users.keys.map(_.toInt).maxOption.getOrElse(0) + 1).toString
          val user = User(
            id = newId,
            name = userFromBody.name,
            email = userFromBody.email,
            age = userFromBody.age,
            isActive = userFromBody.isActive
          )
          users(newId) = user
          write(user)
        }

      case Left(error) =>
        val errorResponse = ErrorResponse(
          error = "validation_failed",
          message = error.message
        )
        write(errorResponse)
    }
  }

  @CaskChez.get(
    "/users",
    RouteSchema(
      summary = Some("List users"),
      description = Some("Retrieves all users"),
      tags = List("users"),
      responses = Map(
        200 -> ApiResponse("Users retrieved successfully", Schema[List[User]])
      )
    )
  )
  def listUsers(validatedRequest: ValidatedRequest): String = {
    val userList = users.values.toList.sortBy(_.id)
    write(userList)
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

  @CaskChez.delete(
    "/users/:id",
    RouteSchema(
      summary = Some("Delete user"),
      description = Some("Deletes a user by their unique identifier"),
      tags = List("users"),
      responses = Map(
        200 -> ApiResponse("User deleted successfully", Schema[User]),
        404 -> ApiResponse("User not found", Schema[ErrorResponse])
      )
    )
  )
  def deleteUser(id: String, validatedRequest: ValidatedRequest): String = {
    users.get(id) match {
      case Some(user) =>
        users.remove(id)
        write(user)
      case None =>
        val error = ErrorResponse(
          error = "user_not_found",
          message = s"User with ID '$id' was not found"
        )
        write(error)
    }
  }

  // Simple health check without validation
  @cask.get("/health")
  def health(): String = {
    """{"status": "ok"}"""
  }

  initialize()
}
