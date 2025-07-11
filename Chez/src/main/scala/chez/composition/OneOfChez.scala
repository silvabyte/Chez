package chez.composition

import chez.Chez
import upickle.default.*

/**
 * OneOf composition schema - validates if the instance is valid against exactly one of the schemas
 */
case class OneOfChez(
    schemas: List[Chez]
) extends Chez {

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()

    schema("oneOf") = ujson.Arr(schemas.map(_.toJsonSchema)*)

    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Validate a value against this oneOf schema
   */
  def validate(value: ujson.Value): List[chez.ValidationError] = {
    // For oneOf, exactly one schema must validate successfully
    val results = schemas.map { schema =>
      // For now, we'll implement basic validation
      // In practice, we'd need to validate the value against each schema
      // This is a placeholder for proper oneOf validation
      // TODO: implement this
      List.empty[chez.ValidationError]
    }

    val successCount = results.count(_.isEmpty)

    if (successCount == 1) {
      List.empty
    } else if (successCount == 0) {
      List(chez.ValidationError.CompositionError("Value does not match any of the schemas in oneOf", "/"))
    } else {
      List(chez.ValidationError.CompositionError("Value matches more than one schema in oneOf", "/"))
    }
  }
}
