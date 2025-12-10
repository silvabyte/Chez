package boogieloops.schema

import boogieloops.schema.primitives.*
import boogieloops.schema.complex.*

/**
 * Type-level computation for mapping Chez schemas to Scala types
 *
 * This uses Scala 3's match types to provide compile-time type safety. For complex types, use Mirror-based Schema derivation with
 * case classes, see ./derivation/SchemaDerivation.scala
 */
type SchemaType[C <: Schema] = C match {
  // Primitive types - fully supported
  case StringSchema => String
  case NumberSchema => Double
  case IntegerSchema => Int
  case BooleanSchema => Boolean
  case NullSchema => Null

  // Complex types - use case classes with Mirror derivation instead
  case ArraySchema[t] => List[SchemaType[t]]

  // Modifiers - fully supported
  case OptionalSchema[t] => Option[SchemaType[t]]
  case NullableSchema[t] => Option[SchemaType[t]]
  case OptionalNullableSchema[t] => Option[SchemaType[t]]
  case DefaultSchema[t] => SchemaType[t]
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
  case ContainsViolation(
      minContains: Option[Int],
      maxContains: Option[Int],
      actualContains: Int,
      path: String
  )
  case MinLengthViolation(min: Int, actual: Int, path: String)
  case MaxLengthViolation(max: Int, actual: Int, path: String)
  case MinPropertiesViolation(min: Int, actual: Int, path: String)
  case MaxPropertiesViolation(max: Int, actual: Int, path: String)
  case MultipleOfViolation(multipleOf: Double, value: Double, path: String)
  case CompositionError(message: String, path: String)
  case ReferenceError(ref: String, path: String)
}
