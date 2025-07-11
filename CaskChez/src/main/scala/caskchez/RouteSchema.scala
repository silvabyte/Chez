package caskchez

import _root_.chez.Chez

/**
 * Core schema definition classes for CaskChez module
 *
 * This file contains the fundamental data structures for defining API route schemas for use with custom Cask endpoints that
 * provide automatic validation.
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
    params: Option[Chez] = None,
    body: Option[Chez] = None,
    query: Option[Chez] = None,
    headers: Option[Chez] = None,
    responses: Map[StatusCode, ApiResponse] = Map.empty
)

/**
 * Simple registry for storing route schemas for OpenAPI generation
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
   * Clear all registered schemas (useful for testing)
   */
  def clear(): Unit = schemas = Map.empty
}
