package caskchez.examples.zerotoapp

import cask._
import caskchez.*
import caskchez.CaskChez.ValidatedRequestReader
import chez.derivation.Schema
import upickle.default.*

/**
 * Zero-to-App: API stub
 *
 * Start the server:
 *   ./mill CaskChez.runMain caskchez.examples.zerotoapp.ZeroToAppApi
 *
 * Follow docs/zero-to-app.md Steps 2–5 to flesh out endpoints and OpenAPI.
 */
object ZeroToAppApi extends cask.MainRoutes {

  // TODO: Replace with a persistent store if desired. This in-memory map is fine for quick start.
  private var users: Map[String, User] = Map.empty

  // POST /users — create a user
  // Docs: Zero to App, Step 2 (Add a minimal API)
  @CaskChez.post(
    "/users",
    RouteSchema(
      summary = Some("Create user"),
      body = Some(Schema[CreateUser]),
      responses = Map(201 -> ApiResponse("Created", Schema[User]))
    )
  )
  def create(req: ValidatedRequest) = {
    // TODO: Replace this minimal logic with the version in docs/zero-to-app.md
    req.getBody[CreateUser] match {
      case Right(in) =>
        val id = (users.size + 1).toString
        val out = User(id, in.name, in.email, in.age)
        users = users.updated(id, out)
        write(out)
      case Left(err) =>
        write(ujson.Obj("error" -> "validation_failed", "message" -> err.message))
    }
  }

  // GET /users — list users
  @CaskChez.get(
    "/users",
    RouteSchema(
      summary = Some("List users"),
      responses = Map(200 -> ApiResponse("OK", Schema[List[User]]))
    )
  )
  def list(req: ValidatedRequest) = {
    // TODO: Add filters/pagination later if needed
    write(users.values.toList)
  }

  // Optional: Add swagger endpoint per docs (Step 5)
  // @CaskChez.swagger("/openapi", OpenAPIConfig(title = "Quickstart API", summary = Some("Zero to App demo")))
  // def openapi(): String = ""

  // Default port to avoid clashing with other examples
  override def port = 8082
  initialize()
}
