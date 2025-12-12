package boogieloops.web.openapi.generators

import _root_.boogieloops.schema.Schema

/**
 * Schema utilities for OpenAPI 3.1.1 generation
 *
 * OpenAPI 3.1.1 uses JSON Schema 2020-12 directly, which means
 * Schema schemas can be used without conversion!
 */
object SchemaConverter {

  /**
   * Generate a meaningful name for a schema component
   */
  def generateSchemaName(schema: Schema, fallback: String = "Schema"): String = {
    val jsonSchema = schema.toJsonSchema

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
   * Check if two Schema schemas are equivalent for deduplication
   */
  def schemasEqual(schema1: Schema, schema2: Schema): Boolean = {
    schema1.toJsonSchema == schema2.toJsonSchema
  }

  /**
   * Calculate hash for schema deduplication
   */
  def schemaHash(schema: Schema): Int = {
    schema.toJsonSchema.hashCode()
  }
}
