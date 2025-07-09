package chez.composition

import chez.Chez
import upickle.default.*

/**
 * Not composition schema - validates if the instance is NOT valid against the schema
 */
case class NotChez(
  schema: Chez
) extends Chez {
  
  override def toJsonSchema: ujson.Value = {
    val schemaObj = ujson.Obj()
    
    schemaObj("not") = schema.toJsonSchema
    
    title.foreach(t => schemaObj("title") = ujson.Str(t))
    description.foreach(d => schemaObj("description") = ujson.Str(d))
    default.foreach(d => schemaObj("default") = d)
    examples.foreach(e => schemaObj("examples") = ujson.Arr(e*))
    
    schemaObj
  }
  
  /**
   * Validate a value against this not schema
   */
  def validate(value: ujson.Value): List[chez.ValidationError] = {
    // For not, the schema must NOT validate successfully
    // For now, we'll implement basic validation
    // In practice, we'd need to validate the value against the schema
    // This is a placeholder for proper not validation
    val errors = List.empty[chez.ValidationError] // Placeholder
    
    if (errors.isEmpty) {
      // Schema validated successfully, so NOT validation fails
      List(chez.ValidationError.CompositionError("Value must NOT match the schema", "/"))
    } else {
      // Schema validation failed, so NOT validation succeeds
      List.empty
    }
  }
}