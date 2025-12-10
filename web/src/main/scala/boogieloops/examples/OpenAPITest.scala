package boogieloops.web.examples

import boogieloops.web.*
import boogieloops.web.openapi.config.OpenAPIConfig
import boogieloops.web.openapi.generators.OpenAPIGenerator
import _root_.boogieloops.schema.*
import upickle.default.*

/**
 * Simple test for OpenAPI 3.1.1 generation functionality
 */
object OpenAPITest {

  def main(args: Array[String]): Unit = {
    // Clear any existing routes
    RouteSchemaRegistry.clear()

    // Create some test schemas
    val userSchema = Schema.Object(
      "name" -> Schema.String(minLength = Some(1)),
      "email" -> Schema.String(format = Some("email")),
      "age" -> Schema.Integer(minimum = Some(0))
    )

    val responseSchema = Schema.Object(
      "id" -> Schema.String(),
      "name" -> Schema.String(),
      "email" -> Schema.String()
    )

    // Register some test routes
    RouteSchemaRegistry.register(
      "/users",
      "POST",
      RouteSchema(
        summary = Some("Create user"),
        description = Some("Create a new user with validation"),
        tags = List("users", "creation"),
        body = Some(userSchema),
        responses = Map(
          201 -> ApiResponse("User created", responseSchema),
          400 -> ApiResponse("Validation error", Schema.Object("error" -> Schema.String()))
        )
      )
    )

    RouteSchemaRegistry.register(
      "/users/{id}",
      "GET",
      RouteSchema(
        summary = Some("Get user"),
        description = Some("Retrieve user by ID"),
        tags = List("users"),
        responses = Map(
          200 -> ApiResponse("User found", responseSchema),
          404 -> ApiResponse("User not found", Schema.Object("error" -> Schema.String()))
        )
      )
    )

    // Generate OpenAPI document
    val config = OpenAPIConfig(
      title = "Test API",
      summary = Some("OpenAPI 3.1.1 Generation Test"),
      description = "Testing boogieloops.web OpenAPI generation with JSON Schema 2020-12",
      version = "1.0.0"
    )

    val openAPIDoc = OpenAPIGenerator.generateDocument(config)

    // Output the generated OpenAPI specification
    println("🎯 Generated OpenAPI 3.1.1 Document:")
    println("=" * 50)
    println(write(openAPIDoc, indent = 2))
    println("=" * 50)

    // Verify key components
    println(s"OpenAPI Version: ${openAPIDoc.openapi}")
    println(s"Title: ${openAPIDoc.info.title}")
    println(s"JSON Schema Dialect: ${openAPIDoc.jsonSchemaDialect}")
    println(s"Number of paths: ${openAPIDoc.paths.map(_.paths.size).getOrElse(0)}")
    println(
      s"Number of components: ${openAPIDoc.components.flatMap(_.schemas).map(_.size).getOrElse(0)}"
    )
    println(s"Number of tags: ${openAPIDoc.tags.map(_.size).getOrElse(0)}")

    println("\n✅ OpenAPI 3.1.1 generation test completed successfully!")
  }
}
