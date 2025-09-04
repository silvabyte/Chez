package caskchez.examples.zerotoapp

import cask._
import scala.annotation.unused
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

  // NOTE: In the real world, you'd use a real database to back this rest API.
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
  def list(@unused req: ValidatedRequest) = {
    // TODO: Add filters/pagination later if needed
    write(users.values.toList)
  }

  // POST /users/:id/interests/infer — derive interests from a profile summary
  // This endpoint demonstrates how to normalize inferred interests into a
  // typed model. For a production-quality version, wire to ChezWiz Agent.
  @CaskChez.post(
    "/users/:id/interests/infer",
    RouteSchema(
      summary = Some("Infer user interests from free text"),
      description = Some("Provide a profile summary text; get normalized interests"),
      body = Some(Schema[ProfileSummary]),
      responses = Map(200 -> ApiResponse("OK", Schema[UserInterests]))
    )
  )
  def inferInterests(id: String, req: ValidatedRequest) = {
    // Look up user (optional here; you might validate existence)
    val _ = id
    req.getBody[ProfileSummary] match {
      case Right(in) =>
        // TODO: Replace the current naive normalization with ChezWiz Agent integration.
        // Pseudocode example (uncomment if you add ChezWiz dependency):
        //
        // import chezwiz.agent.*
        // import chezwiz.agent.providers.OpenAIProvider
        // val agent = Agent(
        //   name = "InterestsNormalizer",
        //   instructions = "Extract and normalize user interests. Use concise, lowercase tags",
        //   provider = new OpenAIProvider(sys.env("OPENAI_API_KEY")),
        //   model = "gpt-4o-mini"
        // )
        // val result = agent.generateObject[UserInterests](
        //   s"""Normalize interests from this profile:\n${in.text}""",
        //   RequestMetadata(userId = Some(id))
        // )
        // result.fold(_ => fallback, _.data)

        // Naive Implementation
        //
        val tokens = in.text.toLowerCase.split("[^a-z0-9]+").filter(_.nonEmpty)
        val keywords = Set("scala", "java", "kotlin", "functional", "backend", "api", "ai", "ml", "devops")
        val hits = tokens.filter(keywords.contains).distinct.toList
        val interests = UserInterests(
          primary = hits.take(3),
          secondary = hits.drop(3).take(5),
          tags = hits
        )
        write(interests)
      case Left(err) =>
        write(ujson.Obj("error" -> "validation_failed", "message" -> err.message))
    }
  }

  // Optional: Add swagger endpoint per docs (Step 5)
  // @CaskChez.swagger("/openapi", OpenAPIConfig(title = "Quickstart API", summary = Some("Zero to App demo")))
  // def openapi(): String = ""

  // Default port to avoid clashing with other examples
  override def port = 8082
  initialize()
}
