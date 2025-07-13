package chez.primitives

import chez.Chez
import upickle.default.*

/**
 * Number schema type with JSON Schema 2020-12 validation keywords
 */
case class NumberChez(
  minimum: Option[Double] = None,
  maximum: Option[Double] = None,
  exclusiveMinimum: Option[Double] = None,
  exclusiveMaximum: Option[Double] = None,
  multipleOf: Option[Double] = None,
  const: Option[Double] = None
) extends Chez {
  
  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj("type" -> ujson.Str("number"))
    
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
   * Validate a number value against this schema
   */
  def validate(value: Double): List[chez.ValidationError] = {
    var errors = List.empty[chez.ValidationError]
    
    // Minimum validation
    minimum.foreach { min =>
      if (value < min) {
        errors = chez.ValidationError.OutOfRange(Some(min), None, value, "/") :: errors
      }
    }
    
    // Maximum validation
    maximum.foreach { max =>
      if (value > max) {
        errors = chez.ValidationError.OutOfRange(None, Some(max), value, "/") :: errors
      }
    }
    
    // Exclusive minimum validation
    exclusiveMinimum.foreach { min =>
      if (value <= min) {
        errors = chez.ValidationError.OutOfRange(Some(min), None, value, "/") :: errors
      }
    }
    
    // Exclusive maximum validation
    exclusiveMaximum.foreach { max =>
      if (value >= max) {
        errors = chez.ValidationError.OutOfRange(None, Some(max), value, "/") :: errors
      }
    }
    
    // Multiple of validation
    multipleOf.foreach { mul =>
      if (value % mul != 0) {
        errors = chez.ValidationError.MultipleOfViolation(mul, value, "/") :: errors
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