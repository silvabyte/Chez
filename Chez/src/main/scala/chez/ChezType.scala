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
 * This uses Scala 3's match types to provide compile-time type safety while maintaining the ability to generate JSON schemas at
 * runtime.
 */
type ChezType[C <: Chez] = C match {
  // Primitive types
  case StringChez  => String
  case NumberChez  => Double
  case IntegerChez => Int
  case BooleanChez => Boolean
  case NullChez    => Null

  // Complex types
  case ArrayChez[t] => List[ChezType[t]]
  case ObjectChez   => ChezObjectType[C]

  // Composition types
  case AnyOfChez => ChezAnyOfType[C]
  case OneOfChez => ChezOneOfType[C]
  case AllOfChez => ChezAllOfType[C]
  case NotChez   => Any // Not schemas are complex to type

  // Conditional types
  case IfThenElseChez => Any // Conditional schemas are complex to type

  // Reference types
  case RefChez        => Any // References need resolution
  case DynamicRefChez => Any // Dynamic references need resolution

  // Modifiers
  case OptionalChez[t]         => Option[ChezType[t]]
  case NullableChez[t]         => Option[ChezType[t]]
  case OptionalNullableChez[t] => Option[ChezType[t]]
  case DefaultChez[t]          => ChezType[t]
}

/**
 * Type-level computation for object types
 * Maps ObjectChez to Map[String, Any] for integration with ObjectReadWriter
 */
type ChezObjectType[C <: ObjectChez] = Map[String, Any]

/**
 * Type-level computation for anyOf types This uses Scala 3's union types
 */
type ChezAnyOfType[C <: AnyOfChez] = Any // We'll implement proper union type handling later

/**
 * Type-level computation for oneOf types This uses Scala 3's union types
 */
type ChezOneOfType[C <: OneOfChez] = Any // We'll implement proper union type handling later

/**
 * Type-level computation for allOf types This requires intersection types
 */
type ChezAllOfType[C <: AllOfChez] = Any // We'll implement proper intersection type handling later

/**
 * Extension methods for type-safe operations
 */
/**
 * Runtime operations for Chez schemas
 *
 * For now, we'll implement basic operations without the complex type-level programming to get a working foundation. We'll enhance
 * this later with proper type safety.
 */
object ChezRuntime {

  /**
   * Serialize a value to JSON using basic upickle serialization
   */
  def toJson[T: ReadWriter](value: T): ujson.Value = {
    writeJs(value)
  }

  /**
   * Serialize a value to JSON string
   */
  def toJsonString[T: ReadWriter](value: T): String = {
    write(value)
  }

  /**
   * Deserialize from JSON with basic validation
   */
  def fromJson[T: ReadWriter](json: ujson.Value): Either[List[ValidationError], T] = {
    Try(read[T](json)) match {
      case Success(value) => Right(value)
      case Failure(e)     => Left(List(ValidationError.ParseError(e.getMessage, "/")))
    }
  }

  /**
   * Deserialize from JSON string with basic validation
   */
  def fromJsonString[T: ReadWriter](jsonStr: String): Either[List[ValidationError], T] = {
    Try(read[T](jsonStr)) match {
      case Success(value) => Right(value)
      case Failure(e)     => Left(List(ValidationError.ParseError(e.getMessage, "/")))
    }
  }

  /**
   * Unsafe deserialization (throws on error)
   */
  def fromJsonUnsafe[T: ReadWriter](json: ujson.Value): T = {
    read[T](json)
  }

  /**
   * Unsafe deserialization from string (throws on error)
   */
  def fromJsonStringUnsafe[T: ReadWriter](jsonStr: String): T = {
    read[T](jsonStr)
  }
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
  case MinLengthViolation(min: Int, actual: Int, path: String)
  case MaxLengthViolation(max: Int, actual: Int, path: String)
  case MinPropertiesViolation(min: Int, actual: Int, path: String)
  case MaxPropertiesViolation(max: Int, actual: Int, path: String)
  case MultipleOfViolation(multipleOf: Double, value: Double, path: String)
  case CompositionError(message: String, path: String)
  case ReferenceError(ref: String, path: String)
}

/**
 * Exception thrown by unsafe operations
 */
class ValidationException(val errors: List[ValidationError]) extends Exception {
  override def getMessage: String = {
    errors.map(_.toString).mkString(", ")
  }
}
