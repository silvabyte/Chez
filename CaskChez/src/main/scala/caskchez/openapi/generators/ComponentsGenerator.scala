package caskchez.openapi.generators

import caskchez.*
import caskchez.openapi.models.*
import caskchez.openapi.config.*
import _root_.chez.Chez
import upickle.default.*

/**
 * Generates OpenAPI Components Object with schema deduplication
 */
object ComponentsGenerator {

  /**
   * Extract reusable components from all routes
   */
  def extractComponents(
      allRoutes: Map[String, RouteSchema],
      config: OpenAPIConfig
  ): ComponentsObject = {
    val schemas = if (config.extractComponents) extractSchemas(allRoutes) else None
    val securitySchemes = extractSecuritySchemes(allRoutes)

    ComponentsObject(
      schemas = schemas,
      securitySchemes = if (securitySchemes.nonEmpty) Some(securitySchemes) else None,
      responses = extractCommonResponses(allRoutes),
      parameters = extractCommonParameters(allRoutes),
      requestBodies = extractCommonRequestBodies(allRoutes)
    )
  }

  /**
   * Extract and deduplicate schemas from all routes
   */
  private def extractSchemas(allRoutes: Map[String, RouteSchema]): Option[Map[String, SchemaObject]] = {
    val allSchemas = collectAllChezSchemas(allRoutes)
    val deduplicatedSchemas = deduplicateSchemas(allSchemas)
    val namedSchemas = generateSchemaNames(deduplicatedSchemas)

    if (namedSchemas.nonEmpty) {
      Some(namedSchemas.map { case (name, chez) =>
        name -> SchemaConverter.convertChezToOpenAPISchema(chez)
      })
    } else None
  }

  /**
   * Collect all Chez schemas from routes
   */
  private def collectAllChezSchemas(allRoutes: Map[String, RouteSchema]): List[Chez] = {
    allRoutes.values.flatMap { schema =>
      List(
        schema.body,
        schema.query,
        schema.headers,
        schema.params
      ).flatten ++ schema.responses.values.map(_.schema) ++
        schema.responses.values.flatMap(_.headers).toList
    }.toList
  }

  /**
   * Deduplicate schemas based on their JSON representation
   */
  private def deduplicateSchemas(schemas: List[Chez]): List[Chez] = {
    schemas
      .groupBy(SchemaConverter.schemaHash)
      .values
      .map(_.head)
      .toList
  }

  /**
   * Generate meaningful names for schemas
   */
  private def generateSchemaNames(schemas: List[Chez]): Map[String, Chez] = {
    val nameCounter = scala.collection.mutable.Map[String, Int]()

    schemas.map { schema =>
      val baseName = SchemaConverter.generateSchemaName(schema)
      val finalName = nameCounter.get(baseName) match {
        case None =>
          nameCounter(baseName) = 1
          baseName
        case Some(count) =>
          nameCounter(baseName) = count + 1
          s"$baseName$count"
      }
      finalName -> schema
    }.toMap
  }

  /**
   * Extract security schemes from routes
   */
  private def extractSecuritySchemes(allRoutes: Map[String, RouteSchema]): Map[String, SecuritySchemeObject] = {
    val allSecurityRequirements = allRoutes.values.flatMap(_.security).toList
    SecurityGenerator.extractSecuritySchemes(allSecurityRequirements)
  }

  /**
   * Extract common response objects
   */
  private def extractCommonResponses(allRoutes: Map[String, RouteSchema]): Option[Map[String, ResponseObject]] = {
    // Find commonly used responses across routes
    val allResponses = allRoutes.values.flatMap(_.responses.values).toList
    val responsesByDescription = allResponses.groupBy(_.description)

    val commonResponses = responsesByDescription
      .filter(_._2.size > 1)
      .map { case (description, responses) =>
        val response = responses.head
        sanitizeResponseName(description) -> ResponseObject(
          description = response.description,
          content = Some(
            Map(
              "application/json" -> MediaTypeObject(
                schema = Some(SchemaConverter.convertChezToOpenAPISchema(response.schema))
              )
            )
          )
        )
      }
      .toMap

    if (commonResponses.nonEmpty) Some(commonResponses) else None
  }

  /**
   * Extract common parameters
   */
  private def extractCommonParameters(allRoutes: Map[String, RouteSchema]): Option[Map[String, ParameterObject]] = {
    // Could extract commonly used parameters across routes
    None
  }

  /**
   * Extract common request bodies
   */
  private def extractCommonRequestBodies(allRoutes: Map[String, RouteSchema]): Option[Map[String, RequestBodyObject]] = {
    // Could extract commonly used request bodies
    None
  }

  private def sanitizeResponseName(description: String): String = {
    description
      .replaceAll("[^a-zA-Z0-9_]", "")
      .split("\\s+")
      .map(_.capitalize)
      .mkString("")
      .take(50) // Limit length
  }
}
