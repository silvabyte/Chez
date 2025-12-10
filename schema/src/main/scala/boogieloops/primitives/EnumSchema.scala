// scalafix:off DisableSyntax.null
// Disabling for entire file because EnumSchema needs to support null as a valid enum value
// according to JSON Schema specification - enums can contain null as one of the allowed values
package boogieloops.schema.primitives

import boogieloops.schema.Schema
import boogieloops.schema.validation.{ValidationResult, ValidationContext}
import upickle.default.*

/**
 * Enum schema type supporting both string-only and mixed data type enums
 *
 * This class provides comprehensive enum support for JSON Schema 2020-12, allowing enums with homogeneous string values or
 * heterogeneous mixed types including strings, numbers, booleans, and null.
 */
case class EnumSchema(
    enumValues: List[ujson.Value]
) extends Schema {

  /**
   * Check if this is a string-only enum (all values are strings)
   */
  def isStringEnum: Boolean = enumValues.forall(_.isInstanceOf[ujson.Str])

  /**
   * Get string values if this is a string-only enum
   */
  def getStringValues: Option[List[String]] = {
    if (isStringEnum) {
      Some(enumValues.map(_.str))
    } else {
      None
    }
  }

  override def toJsonSchema: ujson.Value = {
    val schema = ujson.Obj()

    // For string enums, set type to "string" for better compatibility
    if (isStringEnum) {
      schema("type") = ujson.Str("string")
    }

    // Set the enum values - this is the core of enum schema
    schema("enum") = ujson.Arr(enumValues*)

    // Add metadata fields
    title.foreach(t => schema("title") = ujson.Str(t))
    description.foreach(d => schema("description") = ujson.Str(d))
    default.foreach(d => schema("default") = d)
    examples.foreach(e => schema("examples") = ujson.Arr(e*))

    schema
  }

  /**
   * Check if a value type is compatible with this enum
   */
  def isValidType(value: ujson.Value): Boolean = {
    // Get the types of allowed enum values
    val allowedTypes = enumValues.map(_.getClass).toSet
    allowedTypes.contains(value.getClass)
  }

  /**
   * Get all the distinct types present in this enum
   */
  def getValueTypes: Set[String] = {
    enumValues.map {
      case _: ujson.Str => "string"
      case _: ujson.Num => "number"
      case ujson.True | ujson.False => "boolean"
      case ujson.Null => "null"
      case _: ujson.Obj => "object"
      case _: ujson.Arr => "array"
    }.toSet
  }

  /**
   * Validate a ujson.Value against this enum schema using ValidationResult
   */
  def validateResult(value: ujson.Value): ValidationResult = {
    validate(value, ValidationContext())
  }

  /**
   * Validate a ujson.Value against this enum schema with context using ValidationResult
   */
  override def validate(value: ujson.Value, context: ValidationContext): ValidationResult = {
    if (enumValues.contains(value)) {
      ValidationResult.valid()
    } else {
      val allowedValues = enumValues.map(writeJs(_)).mkString(", ")
      val error =
        boogieloops.schema.ValidationError.TypeMismatch(allowedValues, writeJs(value).toString, context.path)
      ValidationResult.invalid(error)
    }
  }
}

object EnumSchema {

  /**
   * Create a string-only enum from string values
   */
  def fromStrings(values: List[String]): EnumSchema = {
    new EnumSchema(values.map(ujson.Str(_)))
  }

  /**
   * Create a string-only enum from string values (varargs)
   */
  def fromStrings(values: String*): EnumSchema = {
    fromStrings(values.toList)
  }

  /**
   * Create a mixed enum from ujson.Value list
   */
  def fromValues(values: List[ujson.Value]): EnumSchema = {
    EnumSchema(values)
  }

  /**
   * Create a mixed enum from ujson.Value varargs
   */
  def fromValues(values: ujson.Value*): EnumSchema = {
    EnumSchema(values.toList)
  }

  /**
   * Create a mixed enum from different types (convenience method)
   */
  def mixed(values: (String | Int | Boolean | Double | Null)*): EnumSchema = {
    val jsonValues = values.map {
      case s: String => ujson.Str(s)
      case i: Int => ujson.Num(i)
      case d: Double => ujson.Num(d)
      case b: Boolean => if (b) ujson.True else ujson.False
      case null => ujson.Null
    }.toList
    EnumSchema(jsonValues)
  }

  /**
   * Create a numeric enum from numeric values
   */
  def fromNumbers(values: Double*): EnumSchema = {
    EnumSchema(values.map(ujson.Num(_)).toList)
  }

  /**
   * Create an integer enum from integer values
   */
  def fromInts(values: Int*): EnumSchema = {
    EnumSchema(values.map(v => ujson.Num(v.toDouble)).toList)
  }

  /**
   * Create a boolean enum (typically just [true, false])
   */
  def fromBooleans(values: Boolean*): EnumSchema = {
    EnumSchema(values.map(b => if (b) ujson.True else ujson.False).toList)
  }

}
