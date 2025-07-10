package chez.references

import chez.Chez
import upickle.default.*

/**
 * JSON Schema $defs keyword implementation
 * 
 * The $defs keyword provides a location for reusable schema definitions within a schema document.
 * These definitions can be referenced using $ref with a JSON Pointer to the definition.
 * 
 * According to JSON Schema 2020-12, $defs is the standardized way to define reusable schemas
 * (replacing the draft-07 "definitions" keyword).
 * 
 * Example:
 * {
 *   "$defs": {
 *     "User": {
 *       "type": "object",
 *       "properties": {
 *         "name": { "type": "string" }
 *       }
 *     }
 *   },
 *   "type": "object",
 *   "properties": {
 *     "user": { "$ref": "#/$defs/User" }
 *   }
 * }
 */
case class DefsChez(
  definitions: Map[String, Chez]
) extends Chez {
  
  override def $defs: Option[Map[String, Chez]] = Some(definitions)
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()
    
    // Convert the definitions map to JSON Schema format
    if (definitions.nonEmpty) {
      val defsObj = ujson.Obj()
      definitions.foreach { case (name, chez) =>
        defsObj(name) = chez.toJsonSchema
      }
      schema("$defs") = defsObj
    }
    
    // Add meta-data keywords if present
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    
    schema
  }
  
  /**
   * Validate a value against this $defs schema
   * 
   * Note: $defs by itself doesn't validate instances - it only provides definitions.
   * Validation happens when these definitions are referenced via $ref.
   * This method returns empty errors since $defs alone doesn't constrain values.
   */
  def validate(value: ujson.Value): List[chez.ValidationError] = {
    // $defs itself doesn't validate anything - it just provides definitions
    // The actual validation happens when the definitions are referenced
    List.empty
  }
  
  /**
   * Get a definition by name
   */
  def getDefinition(name: String): Option[Chez] = {
    definitions.get(name)
  }
  
  /**
   * Check if a definition exists
   */
  def hasDefinition(name: String): Boolean = {
    definitions.contains(name)
  }
  
  /**
   * Get all definition names
   */
  def definitionNames: Set[String] = {
    definitions.keySet
  }
}