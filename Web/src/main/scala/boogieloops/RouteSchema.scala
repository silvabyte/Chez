package boogieloops.web

import _root_.boogieloops.schema.Schema

/**
 * Core schema definition classes for boogieloops.web module
 *
 * This file contains the fundamental data structures for defining API route schemas for use with custom Cask endpoints that
 * provide automatic validation.
 */

/**
 * Represents a single API response schema with description and optional headers
 */
case class ApiResponse(
    description: String,
    schema: Schema,
    headers: Option[Schema] = None
)

/**
 * Security requirement definitions for OpenAPI spec
 */
sealed trait SecurityRequirement

object SecurityRequirement {
  case class ApiKey(name: String = "apiKey", in: String = "header") extends SecurityRequirement
  case class Bearer(format: String = "JWT") extends SecurityRequirement
  case class Basic() extends SecurityRequirement
  // TODO: add support for OAuth 2.1 dynamic client registration
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
 * Complete route schema definition containing validation and documentation information
 */
case class RouteSchema(
    description: Option[String] = None,
    tags: List[String] = List.empty,
    summary: Option[String] = None,
    security: List[SecurityRequirement] = List.empty,
    params: Option[Schema] = None,
    body: Option[Schema] = None,
    query: Option[Schema] = None,
    headers: Option[Schema] = None,
    responses: Map[StatusCode, ApiResponse] = Map.empty
)

/**
 * Simple registry for storing route schemas for OpenAPI generation
 */
object RouteSchemaRegistry {

  // scalafix:off DisableSyntax.var
  // Disabling because a mutable registry is needed to dynamically register route schemas at runtime
  // as Cask endpoints are discovered and initialized
  @volatile private var _schemas = Map[String, RouteSchema]()
  // scalafix:on DisableSyntax.var

  /**
   * Register a route schema with a given path and method
   */
  def register(path: String, method: String, schema: RouteSchema): Unit = {
    val key = s"$method:$path"
    _schemas = _schemas.updated(key, schema)
  }

  /**
   * Get all registered schemas
   */
  def getAll: Map[String, RouteSchema] = _schemas.toMap

  /**
   * Clear all registered schemas (useful for testing)
   */
  def clear(): Unit = {
    _schemas = Map.empty
  }
}
