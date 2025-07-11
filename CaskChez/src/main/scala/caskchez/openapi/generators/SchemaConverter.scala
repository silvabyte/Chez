package caskchez.openapi.generators

import _root_.chez.Chez
import caskchez.openapi.models.*
import caskchez.openapi.config.*
import upickle.default.*

/**
 * Converts Chez schemas to OpenAPI 3.1.1 Schema Objects
 * 
 * OpenAPI 3.1.1 uses JSON Schema 2020-12 directly, which means
 * Chez schemas can be used without conversion!
 */
object SchemaConverter {
  
  /**
   * Convert Chez schema to OpenAPI Schema Object
   * 
   * This leverages OpenAPI 3.1.1's native JSON Schema 2020-12 support
   */
  def convertChezToOpenAPISchema(chezSchema: Chez): SchemaObject = {
    SchemaObject(
      // Direct JSON Schema 2020-12 compatibility - no conversion needed!
      schema = chezSchema.toJsonSchema,
      
      // Extract OpenAPI-specific annotations if available
      example = extractExampleFromChez(chezSchema),
      examples = extractExamplesFromChez(chezSchema),
      externalDocs = extractExternalDocsFromChez(chezSchema)
    )
  }
  
  /**
   * Create a schema reference for component reuse
   */
  def createSchemaRef(schemaName: String): SchemaObject = {
    SchemaObject(
      schema = ujson.Obj("$ref" -> s"#/components/schemas/$schemaName")
    )
  }
  
  /**
   * Convert Chez schema directly to ujson.Value (for inline use)
   * No conversion needed - direct JSON Schema 2020-12 support
   */
  def preserveChezSchemaIntegrity(chez: Chez): ujson.Value = {
    chez.toJsonSchema // Direct use in OpenAPI 3.1.1
  }
  
  // Helper methods to extract OpenAPI-specific metadata
  
  private def extractExampleFromChez(chez: Chez): Option[ujson.Value] = {
    // Try to extract example from Chez schema
    val jsonSchema = chez.toJsonSchema
    jsonSchema.objOpt.flatMap(_.get("examples")).flatMap(_.arrOpt).flatMap(_.headOption)
  }
  
  private def extractExamplesFromChez(chez: Chez): Option[List[ujson.Value]] = {
    // Try to extract multiple examples from Chez schema
    val jsonSchema = chez.toJsonSchema
    jsonSchema.objOpt.flatMap(_.get("examples")).flatMap(_.arrOpt).map(_.toList)
  }
  
  private def extractExternalDocsFromChez(chez: Chez): Option[ExternalDocumentationObject] = {
    // Could be extended to extract external documentation from Chez metadata
    None
  }
  
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