package caskchez.openapi.generators

import _root_.chez.Chez

/**
 * Schema utilities for OpenAPI 3.1.1 generation
 *
 * OpenAPI 3.1.1 uses JSON Schema 2020-12 directly, which means
 * Chez schemas can be used without conversion!
 */
object SchemaConverter {

  /**
   * Generate a meaningful name for a schema component
   */
  def generateSchemaName(chez: Chez, fallback: String = "Schema"): String = {
    val jsonSchema = chez.toJsonSchema

    // Try to extract title from schema
    jsonSchema.objOpt
      .flatMap(_.get("title"))
      .flatMap(_.strOpt)
      .map(sanitizeSchemaName)
      .getOrElse {
        // Fallback to type-based naming
        jsonSchema.objOpt
          .flatMap(_.get("type"))
          .flatMap(_.strOpt)
          .map(t => s"${t.capitalize}$fallback")
          .getOrElse(fallback)
      }
  }

  /**
   * Sanitize schema name for use as component key
   */
  private def sanitizeSchemaName(name: String): String = {
    name.replaceAll("[^a-zA-Z0-9_]", "")
      .split("\\s+")
      .map(_.capitalize)
      .mkString("")
  }

  /**
   * Check if two Chez schemas are equivalent for deduplication
   */
  def schemasEqual(schema1: Chez, schema2: Chez): Boolean = {
    schema1.toJsonSchema == schema2.toJsonSchema
  }

  /**
   * Calculate hash for schema deduplication
   */
  def schemaHash(chez: Chez): Int = {
    chez.toJsonSchema.hashCode()
  }
}
