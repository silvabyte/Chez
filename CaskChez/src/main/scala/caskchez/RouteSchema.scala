package caskchez

import chez.*
import scala.annotation.StaticAnnotation

/**
 * Core schema definition classes for CaskChez module
 *
 * This file contains the fundamental data structures for defining API route schemas in a Fastify-like manner, including request
 * validation, response schemas, and metadata.
 */

/**
 * Represents a single API response schema with description and optional headers
 */
case class ApiResponse(
    description: String,
    schema: Chez,
    headers: Option[Chez] = None
)

/**
 * Security requirement definitions for OpenAPI spec
 */
sealed trait SecurityRequirement

object SecurityRequirement {
  case class ApiKey(name: String = "apiKey", in: String = "header") extends SecurityRequirement
  case class Bearer(format: String = "JWT") extends SecurityRequirement
  case class Basic() extends SecurityRequirement
  case class OAuth2(flows: Map[String, String] = Map.empty) extends SecurityRequirement

  def apiKey(name: String = "apiKey", in: String = "header"): ApiKey = ApiKey(name, in)
  def bearer(format: String = "JWT"): Bearer = Bearer(format)
  def basic(): Basic = Basic()
  def oauth2(flows: Map[String, String] = Map.empty): OAuth2 = OAuth2(flows)
}

/**
 * Status code type for response definitions
 */
type StatusCode = Int | String

/**
 * Complete route schema definition containing all possible validation and documentation information
 */
case class RouteSchema(
    description: Option[String] = None,
    tags: List[String] = List.empty,
    summary: Option[String] = None,
    security: List[SecurityRequirement] = List.empty,
    params: Option[Chez] = None,
    body: Option[Chez] = None,
    query: Option[Chez] = None,
    headers: Option[Chez] = None,
    responses: Map[StatusCode, ApiResponse] = Map.empty
) {

  /**
   * Check if this route schema has any validation requirements
   */
  def hasValidation: Boolean = {
    params.isDefined || body.isDefined || query.isDefined || headers.isDefined
  }

  /**
   * Get all schemas referenced in this route for dependency analysis
   */
  def getAllSchemas: List[Chez] = {
    List(params, body, query, headers).flatten ++ responses.values.map(_.schema).toList
  }

  /**
   * Convert to a map for easier processing
   */
  def toMap: Map[String, Any] = Map(
    "description" -> description,
    "tags" -> tags,
    "summary" -> summary,
    "security" -> security,
    "params" -> params,
    "body" -> body,
    "query" -> query,
    "headers" -> headers,
    "responses" -> responses
  ).filter(_._2 != None)
}

/**
 * Annotation for defining route schemas
 *
 * This annotation can be applied to Cask route methods to define:
 *   - Request validation schemas (params, body, query, headers)
 *   - Response schemas with status codes
 *   - API documentation metadata
 *
 * Example usage:
 * ```scala
 * @schema(
 *   description = "Create a new user",
 *   tags = List("users"),
 *   body = Some(createUserSchema),
 *   responses = Map(
 *     201 -> ApiResponse("User created", userSchema),
 *     400 -> ApiResponse("Validation error", errorSchema)
 *   )
 * )
 * @cask.postJson("/users")
 * def createUser() = { ... }
 * ```
 */
case class schema(
    description: String = "",
    tags: List[String] = List.empty,
    summary: String = "",
    security: List[SecurityRequirement] = List.empty,
    params: Option[Chez] = None,
    body: Option[Chez] = None,
    query: Option[Chez] = None,
    headers: Option[Chez] = None,
    responses: Map[StatusCode, ApiResponse] = Map.empty
) extends StaticAnnotation {

  /**
   * Convert annotation to RouteSchema instance
   */
  def toRouteSchema: RouteSchema = RouteSchema(
    description = if (description.nonEmpty) Some(description) else None,
    tags = tags,
    summary = if (summary.nonEmpty) Some(summary) else None,
    security = security,
    params = params,
    body = body,
    query = query,
    headers = headers,
    responses = responses
  )
}

/**
 * Registry for storing route schemas extracted from annotations
 *
 * This will be used to collect all route schemas for OpenAPI generation
 */
object RouteSchemaRegistry {

  private var schemas = Map[String, RouteSchema]()

  /**
   * Register a route schema with a given path and method
   */
  def register(path: String, method: String, schema: RouteSchema): Unit = {
    val key = s"$method:$path"
    schemas = schemas.updated(key, schema)
  }

  /**
   * Get all registered schemas
   */
  def getAll: Map[String, RouteSchema] = schemas.toMap

  /**
   * Get schema for a specific route
   */
  def get(path: String, method: String): Option[RouteSchema] = {
    val key = s"$method:$path"
    schemas.get(key)
  }

  /**
   * Clear all registered schemas (useful for testing)
   */
  def clear(): Unit = schemas = Map.empty

  /**
   * Get all unique tags from registered schemas
   */
  def getAllTags: Set[String] = {
    schemas.values.flatMap(_.tags).toSet
  }

  /**
   * Get all schemas that use a specific tag
   */
  def getByTag(tag: String): Map[String, RouteSchema] = {
    schemas.filter(_._2.tags.contains(tag)).toMap
  }
}
