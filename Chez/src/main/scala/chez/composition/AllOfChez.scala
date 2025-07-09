package chez.composition

import chez.Chez
import upickle.default.*

/**
 * AllOf composition schema - validates if the instance is valid against all of the schemas
 */
case class AllOfChez(
  schemas: List[Chez]
) extends Chez {
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()
    
    schema("allOf") = ujson.Arr(schemas.map(_.toJsonSchema)*)
    
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))
    
    schema
  }
  
  /**
   * Validate a value against this allOf schema
   */
  def validate(value: ujson.Value): List[chez.ValidationError] = {
    // For allOf, all schemas must validate successfully
    val results = schemas.map { schema =>
      // For now, we'll implement basic validation
      // In practice, we'd need to validate the value against each schema
      // This is a placeholder for proper allOf validation
      List.empty[chez.ValidationError]
    }
    
    val allErrors = results.flatten
    
    if (allErrors.isEmpty) {
      List.empty
    } else {
      chez.ValidationError.CompositionError("Value does not match all schemas in allOf", "/") :: allErrors
    }
  }
}