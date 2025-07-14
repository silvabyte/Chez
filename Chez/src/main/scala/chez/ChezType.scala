package chez

import upickle.default.*
import chez.primitives.*
import chez.complex.*
import chez.composition.*
import chez.references.*

import scala.util.{Try, Success, Failure}

/**
 * Type-level computation for mapping Chez schemas to Scala types
 *
 * This uses Scala 3's match types to provide compile-time type safety. For complex types, use Mirror-based Schema derivation with
 * case classes, see ./derivation/SchemaDerivation.scala
 */
type ChezType[C <: Chez] = C match {
  // Primitive types - fully supported
  case StringChez => String
  case NumberChez => Double
  case IntegerChez => Int
  case BooleanChez => Boolean
  case NullChez => Null

  // Complex types - use case classes with Mirror derivation instead
  case ArrayChez[t] => List[ChezType[t]]

  // Modifiers - fully supported
  case OptionalChez[t] => Option[ChezType[t]]
  case NullableChez[t] => Option[ChezType[t]]
  case OptionalNullableChez[t] => Option[ChezType[t]]
  case DefaultChez[t] => ChezType[t]
}

/**
 * Validation error types
 */
enum ValidationError {
  case TypeMismatch(expected: String, actual: String, path: String)
  case MissingField(field: String, path: String)
  case InvalidFormat(format: String, value: String, path: String)
  case OutOfRange(min: Option[Double], max: Option[Double], value: Double, path: String)
  case ParseError(message: String, path: String)
  case PatternMismatch(pattern: String, value: String, path: String)
  case AdditionalProperty(property: String, path: String)
  case UniqueViolation(path: String)
  case MinItemsViolation(min: Int, actual: Int, path: String)
  case MaxItemsViolation(max: Int, actual: Int, path: String)
  case ContainsViolation(minContains: Option[Int], maxContains: Option[Int], actualContains: Int, path: String)
  case MinLengthViolation(min: Int, actual: Int, path: String)
  case MaxLengthViolation(max: Int, actual: Int, path: String)
  case MinPropertiesViolation(min: Int, actual: Int, path: String)
  case MaxPropertiesViolation(max: Int, actual: Int, path: String)
  case MultipleOfViolation(multipleOf: Double, value: Double, path: String)
  case CompositionError(message: String, path: String)
  case ReferenceError(ref: String, path: String)
}
