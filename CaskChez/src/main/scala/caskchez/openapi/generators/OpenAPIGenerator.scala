package caskchez.openapi.generators

import caskchez.*
import caskchez.openapi.models.*
import caskchez.openapi.config.*
import _root_.chez.Chez
import upickle.default.*

/**
 * Main OpenAPI 3.1.1 document generator
 */
object OpenAPIGenerator {

  // scalafix:off DisableSyntax.var
  // Disabling because mutable cache is needed for performance optimization - avoids regenerating
  // the OpenAPI document on every request when the registry hasn't changed
  @volatile private var _documentCache: Option[(OpenAPIDocument, Int)] = None
  // scalafix:on DisableSyntax.var

  /**
   * Generate complete OpenAPI 3.1.1 document from registered routes
   */
  def generateDocument(config: OpenAPIConfig = OpenAPIConfig()): OpenAPIDocument = {
    val registryHash = RouteSchemaRegistry.getAll.hashCode()

    // Use cache if registry hasn't changed
    _documentCache match {
      case Some((doc, hash)) if hash == registryHash => doc
      case _ =>
        val newDoc = generateFreshDocument(config)
        _documentCache = Some((newDoc, registryHash))
        newDoc
    }
  }

  /**
   * Generate fresh OpenAPI document from current registry state
   */
  private def generateFreshDocument(config: OpenAPIConfig): OpenAPIDocument = {
    val allRoutes = RouteSchemaRegistry.getAll

    OpenAPIDocument(
      openapi = "3.1.1",
      info = InfoObject(
        title = config.title,
        summary = config.summary,
        description = config.description,
        version = config.version,
        termsOfService = config.termsOfService,
        contact = config.contact,
        license = config.license
      ),
      jsonSchemaDialect = config.jsonSchemaDialect,
      servers = if (config.servers.nonEmpty) Some(config.servers) else None,
      paths = if (allRoutes.nonEmpty)
        Some(PathsGenerator.convertPathsFromRegistry(allRoutes, config))
      else None,
      components = if (config.extractComponents && allRoutes.nonEmpty)
        Some(ComponentsGenerator.extractComponents(allRoutes, config))
      else None,
      tags = if (allRoutes.nonEmpty) Some(TagGenerator.extractUniqueTags(allRoutes)) else None,
      externalDocs = config.externalDocs
    )
  }

  /**
   * Clear the document cache (useful for testing or manual refresh)
   */
  def clearCache(): Unit = {
    _documentCache = None
  }
}
