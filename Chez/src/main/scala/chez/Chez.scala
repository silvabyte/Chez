package chez

import upickle.default.*
import chez.primitives.*
import chez.complex.*
import chez.composition.*
import chez.references.*
import chez.modifiers.*

/**
 * Core Chez trait - represents a JSON Schema with compile-time type information
 *
 * This is the foundation trait for all Chez types, providing JSON Schema 2020-12 compliance while maintaining compile-time type
 * safety through Scala 3's type system.
 */
trait Chez {
  // Core vocabulary keywords (JSON Schema 2020-12)
  def $schema: Option[String] = None
  def $id: Option[String] = None
  def $ref: Option[String] = None
  def $defs: Option[Map[String, Chez]] = None
  def $dynamicRef: Option[String] = None
  def $dynamicAnchor: Option[String] = None
  def $vocabulary: Option[Map[String, Boolean]] = None
  def $comment: Option[String] = None

  // Meta-data vocabulary keywords
  def title: Option[String] = None
  def description: Option[String] = None
  def default: Option[ujson.Value] = None
  def examples: Option[List[ujson.Value]] = None
  def readOnly: Option[Boolean] = None
  def writeOnly: Option[Boolean] = None
  def deprecated: Option[Boolean] = None

  // Generate JSON Schema representation
  def toJsonSchema: ujson.Value

  // Modifier methods for chaining
  def optional: Chez = OptionalChez(this)
  def nullable: Chez = NullableChez(this)
  def withDefault(value: ujson.Value): Chez = DefaultChez(this, value)
  def withTitle(title: String): Chez = TitleChez(this, title)
  def withDescription(desc: String): Chez = DescriptionChez(this, desc)
  def withSchema(schema: String): Chez = SchemaChez(this, schema)
  def withId(id: String): Chez = IdChez(this, id)
  def withDefs(defs: (String, Chez)*): Chez = chez.modifiers.DefsChez(this, defs.toMap)
  def withExamples(examples: ujson.Value*): Chez = ExamplesChez(this, examples.toList)
}

/**
 * Modifier wrapper for optional fields
 */
case class OptionalChez[T <: Chez](underlying: T) extends Chez {
  override def toJsonSchema: ujson.Value = underlying.toJsonSchema

  override def nullable: Chez = OptionalNullableChez(underlying)
}

/**
 * Modifier wrapper for nullable fields
 */
case class NullableChez[T <: Chez](underlying: T) extends Chez {
  override def toJsonSchema: ujson.Value = {
    val base = underlying.toJsonSchema
    base("type") = ujson.Arr(base("type"), ujson.Str("null"))
    base
  }

  override def optional: Chez = OptionalNullableChez(underlying)
}

/**
 * Modifier wrapper for optional AND nullable fields
 */
case class OptionalNullableChez[T <: Chez](underlying: T) extends Chez {
  override def toJsonSchema: ujson.Value = {
    val base = underlying.toJsonSchema
    base("type") = ujson.Arr(base("type"), ujson.Str("null"))
    base
  }
}

/**
 * Modifier wrapper for fields with default values
 */
case class DefaultChez[T <: Chez](underlying: T, defaultValue: ujson.Value) extends Chez {
  override def toJsonSchema: ujson.Value = {
    val base = underlying.toJsonSchema
    base("default") = defaultValue
    base
  }

  override def optional: Chez = OptionalChez(this)
  override def nullable: Chez = NullableChez(this)
}

/**
 * Companion object with factory methods
 */
object Chez {
  // JSON Schema 2020-12 meta-schema URL
  val MetaSchemaUrl = "https://json-schema.org/draft/2020-12/schema"

  // Convenience factory methods
  def String(
      minLength: Option[Int] = None,
      maxLength: Option[Int] = None,
      pattern: Option[String] = None,
      format: Option[String] = None,
      const: Option[String] = None,
      enumValues: Option[List[String]] = None
  ): StringChez = StringChez(minLength, maxLength, pattern, format, const, enumValues)

  def Number(
      minimum: Option[Double] = None,
      maximum: Option[Double] = None,
      exclusiveMinimum: Option[Double] = None,
      exclusiveMaximum: Option[Double] = None,
      multipleOf: Option[Double] = None
  ): NumberChez = NumberChez(minimum, maximum, exclusiveMinimum, exclusiveMaximum, multipleOf)

  def Integer(
      minimum: Option[Int] = None,
      maximum: Option[Int] = None,
      exclusiveMinimum: Option[Int] = None,
      exclusiveMaximum: Option[Int] = None,
      multipleOf: Option[Int] = None,
      const: Option[Int] = None,
      enumValues: Option[List[Int]] = None
  ): IntegerChez = IntegerChez(minimum, maximum, exclusiveMinimum, exclusiveMaximum, multipleOf, const, enumValues)

  def Boolean(const: Option[Boolean] = None): BooleanChez = BooleanChez(const)

  def Null(): NullChez = NullChez()

  def Array[T <: Chez](
      items: T,
      minItems: Option[Int] = None,
      maxItems: Option[Int] = None,
      uniqueItems: Option[Boolean] = None,
      prefixItems: Option[List[Chez]] = None
  ): ArrayChez[T] = ArrayChez(items, minItems, maxItems, uniqueItems, prefixItems)

  def Object(
      properties: (String, Chez)*
  ): ObjectChez = ObjectChez(properties.toMap)

  def Object(
      properties: Map[String, Chez],
      required: Set[String] = Set.empty,
      minProperties: Option[Int] = None,
      maxProperties: Option[Int] = None,
      additionalProperties: Option[Boolean] = None,
      patternProperties: Map[String, Chez] = Map.empty,
      propertyNames: Option[Chez] = None
  ): ObjectChez =
    ObjectChez(properties, required, minProperties, maxProperties, additionalProperties, patternProperties, propertyNames)

  // Composition keywords
  def AnyOf(schemas: Chez*): AnyOfChez = AnyOfChez(schemas.toList)
  def OneOf(schemas: Chez*): OneOfChez = OneOfChez(schemas.toList)
  def AllOf(schemas: Chez*): AllOfChez = AllOfChez(schemas.toList)
  def Not(schema: Chez): NotChez = NotChez(schema)

  // Conditional schemas
  def If(condition: Chez, thenSchema: Chez, elseSchema: Chez): IfThenElseChez =
    IfThenElseChez(condition, Some(thenSchema), Some(elseSchema))
  def If(condition: Chez, thenSchema: Chez): IfThenElseChez = IfThenElseChez(condition, Some(thenSchema), None)

  // References
  def Ref(ref: String): RefChez = RefChez(ref)
  def DynamicRef(ref: String): DynamicRefChez = DynamicRefChez(ref)
}
