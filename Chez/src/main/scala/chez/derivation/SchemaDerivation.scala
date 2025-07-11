package chez.derivation

import scala.deriving.*
import scala.compiletime.*
import chez.*
import chez.primitives.*
import chez.complex.*
import upickle.default.*

/**
 * Type class for automatically deriving Chez schemas from case class definitions
 *
 * Usage: case class User(id: String, name: String) derives Schema
 */
trait Schema[T] {
  def schema: Chez
}

object Schema {

  /**
   * Get the schema for a type T
   */
  def apply[T](using s: Schema[T]): Chez = s.schema

  /**
   * Create a Schema instance from a Chez schema
   */
  def instance[T](chez: Chez): Schema[T] = new Schema[T]:
    def schema: Chez = chez

  /**
   * Derive a Schema instance using Mirror-based reflection
   *
   * This is the core derivation method that analyzes case class structure and generates the corresponding Chez schema
   * automatically.
   */
  inline def derived[T](using m: Mirror.Of[T]): Schema[T] =
    val chez = inline m match
      case p: Mirror.ProductOf[T] => deriveProduct[T](p)
      case s: Mirror.SumOf[T]     => deriveSum[T](s)
    instance[T](chez)

  /**
   * Derive schema for product types (case classes)
   */
  inline def deriveProduct[T](p: Mirror.ProductOf[T]): Chez =
    val typeName = constValue[p.MirroredLabel]
    val elemLabels = getElemLabels[p.MirroredElemLabels]
    val elemSchemas = getElemSchemas[T, p.MirroredElemTypes]
    val properties = elemLabels.zip(elemSchemas).toMap
    val required = getRequiredFields[p.MirroredElemTypes](elemLabels)

    chez.Chez.Object(
      properties = properties,
      required = required
    )

  /**
   * Derive schema for sum types (enums/sealed traits)
   */
  inline def deriveSum[T](s: Mirror.SumOf[T]): Chez =
    val elemSchemas = getElemSchemas[T, s.MirroredElemTypes]
    // For now, use the first alternative for sum types
    // TODO: Implement proper AnyOf support
    if elemSchemas.nonEmpty then elemSchemas.head else chez.Chez.String()

  /**
   * Get element labels as List[String]
   */
  inline def getElemLabels[Labels <: Tuple]: List[String] =
    inline erasedValue[Labels] match
      case _: (head *: tail) =>
        constValue[head].asInstanceOf[String] :: getElemLabels[tail]
      case _: EmptyTuple => Nil

  /**
   * Get element schemas by recursively deriving or summoning Schema instances
   */
  inline def getElemSchemas[T, Elems <: Tuple]: List[Chez] =
    inline erasedValue[Elems] match
      case _: (head *: tail) =>
        getElemSchema[T, head] :: getElemSchemas[T, tail]
      case _: EmptyTuple => Nil

  /**
   * Get schema for a single element type
   */
  inline def getElemSchema[T, Elem]: Chez =
    inline erasedValue[Elem] match
      case _: String    => chez.Chez.String()
      case _: Int       => chez.Chez.Integer()
      case _: Long      => chez.Chez.Integer()
      case _: Double    => chez.Chez.Number()
      case _: Float     => chez.Chez.Number()
      case _: Boolean   => chez.Chez.Boolean()
      case _: Option[t] => getElemSchema[T, t].optional
      case _: List[t]   => chez.Chez.Array(getElemSchema[T, t])
      case _            =>
        // For complex types, try to summon a Schema instance
        summonInline[Schema[Elem]].schema

  /**
   * Determine required fields (non-Option types)
   */
  inline def getRequiredFields[Elems <: Tuple](labels: List[String]): Set[String] =
    inline erasedValue[Elems] match
      case _: (head *: tail) =>
        val requiredTail = getRequiredFields[tail](labels.tail)
        inline erasedValue[head] match
          case _: Option[_] => requiredTail
          case _            => requiredTail + labels.head
      case _: EmptyTuple => Set.empty

  /**
   * Basic Schema instances for primitive types
   */
  given Schema[String] = instance(chez.Chez.String())
  given Schema[Int] = instance(chez.Chez.Integer())
  given Schema[Long] = instance(chez.Chez.Integer())
  given Schema[Double] = instance(chez.Chez.Number())
  given Schema[Float] = instance(chez.Chez.Number())
  given Schema[Boolean] = instance(chez.Chez.Boolean())

  given [T](using s: Schema[T]): Schema[Option[T]] = instance(s.schema.optional)
  given [T](using s: Schema[T]): Schema[List[T]] = instance(chez.Chez.Array(s.schema))

}

/**
 * Enhanced ReadWriter derivation that combines Schema with upickle ReadWriter
 */
object ValidatedReadWriter {

  /**
   * Create a ReadWriter that validates against the derived schema
   */
  def derived[T](using s: Schema[T], rw: ReadWriter[T]): ReadWriter[T] =
    readwriter[ujson.Value].bimap[T](
      // Writer: T -> ujson.Value (use case class ReadWriter)
      value => writeJs(value),
      // Reader: ujson.Value -> T (validate against schema, then deserialize)
      json => {
        // TODO: Add actual schema validation here
        // For now, just deserialize with the case class ReadWriter
        read[T](json)
      }
    )
}
