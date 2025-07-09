package chez.composition

import chez.Chez
import upickle.default.*

/**
 * AnyOf composition schema - validates if the instance is valid against any of the schemas
 */
case class AnyOfChez(
  schemas: List[Chez]
) extends Chez {
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()
    
    schema("anyOf") = ujson.Arr(schemas.map(_.toJsonSchema)*)
    
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))
    
    schema
  }
  
  /**
   * Validate a value against this anyOf schema
   */
  def validate(value: ujson.Value): List[chez.ValidationError] = {
    // For anyOf, at least one schema must validate successfully
    val results = schemas.map { schema =>
      // For now, we'll implement basic validation
      // In practice, we'd need to validate the value against each schema
      // This is a placeholder for proper anyOf validation
      List.empty[chez.ValidationError]
    }
    
    // If at least one schema validates successfully, return no errors
    if (results.exists(_.isEmpty)) {
      List.empty
    } else {
      // If all schemas fail, return composition error
      List(chez.ValidationError.CompositionError("Value does not match any of the schemas in anyOf", "/"))
    }
  }
}