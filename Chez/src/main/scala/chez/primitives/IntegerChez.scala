package chez.primitives

import chez.Chez
import upickle.default.*

/**
 * Integer schema type with JSON Schema 2020-12 validation keywords
 */
case class IntegerChez(
  minimum: Option[Int] = None,
  maximum: Option[Int] = None,
  exclusiveMinimum: Option[Int] = None,
  exclusiveMaximum: Option[Int] = None,
  multipleOf: Option[Int] = None,
  const: Option[Int] = None
) extends Chez {
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("integer"))
    
    minimum.foreach(min => schema("minimum") = ujson.Num(min))
    maximum.foreach(max => schema("maximum") = ujson.Num(max))
    exclusiveMinimum.foreach(min => schema("exclusiveMinimum") = ujson.Num(min))
    exclusiveMaximum.foreach(max => schema("exclusiveMaximum") = ujson.Num(max))
    multipleOf.foreach(mul => schema("multipleOf") = ujson.Num(mul))
    const.foreach(c => schema("const") = ujson.Num(c))
    
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))
    
    schema
  }
  
  /**
   * Validate an integer value against this schema
   */
  def validate(value: Int): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]
    
    // Minimum validation
    minimum.foreach { min =>
      if (value < min) {
        errors = chez.ValidationError.OutOfRange(Some(min.toDouble), None, value.toDouble, "/") :: errors
      }
    }
    
    // Maximum validation
    maximum.foreach { max =>
      if (value > max) {
        errors = chez.ValidationError.OutOfRange(None, Some(max.toDouble), value.toDouble, "/") :: errors
      }
    }
    
    // Exclusive minimum validation
    exclusiveMinimum.foreach { min =>
      if (value <= min) {
        errors = chez.ValidationError.OutOfRange(Some(min.toDouble), None, value.toDouble, "/") :: errors
      }
    }
    
    // Exclusive maximum validation
    exclusiveMaximum.foreach { max =>
      if (value >= max) {
        errors = chez.ValidationError.OutOfRange(None, Some(max.toDouble), value.toDouble, "/") :: errors
      }
    }
    
    // Multiple of validation
    multipleOf.foreach { mul =>
      if (value % mul != 0) {
        errors = chez.ValidationError.MultipleOfViolation(mul.toDouble, value.toDouble, "/") :: errors
      }
    }
    
    // Const validation
    const.foreach { c =>
      if (value != c) {
        errors = chez.ValidationError.TypeMismatch(c.toString, value.toString, "/") :: errors
      }
    }
    
    
    errors.reverse
  }
}