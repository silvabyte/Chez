package chez.derivation

import scala.deriving.*
import scala.compiletime.*
import scala.quoted.*
import chez.*

import chez.validation.ValidationContext
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

  // ========================================
  // Annotation Type Aliases to be able to do @Schema.title("...") etc.
  // ========================================

  type title = SchemaAnnotations.title
  type description = SchemaAnnotations.description
  type format = SchemaAnnotations.format
  type minLength = SchemaAnnotations.minLength
  type maxLength = SchemaAnnotations.maxLength
  type minimum = SchemaAnnotations.minimum
  type maximum = SchemaAnnotations.maximum
  type pattern = SchemaAnnotations.pattern
  type minItems = SchemaAnnotations.minItems
  type maxItems = SchemaAnnotations.maxItems
  type uniqueItems = SchemaAnnotations.uniqueItems
  type multipleOf = SchemaAnnotations.multipleOf
  type exclusiveMinimum = SchemaAnnotations.exclusiveMinimum
  type exclusiveMaximum = SchemaAnnotations.exclusiveMaximum
  type enumValues = SchemaAnnotations.enumValues
  type const = SchemaAnnotations.const
  type default = SchemaAnnotations.default
  type examples = SchemaAnnotations.examples
  type readOnly = SchemaAnnotations.readOnly
  type writeOnly = SchemaAnnotations.writeOnly
  type deprecated = SchemaAnnotations.deprecated

  /**
   * Derive a Schema instance using Mirror-based reflection with annotation processing
   *
   * This is the enhanced core derivation method that analyzes case class structure, processes annotations, and generates the
   * corresponding Chez schema automatically with rich metadata.
   */
  inline def derived[T](using m: Mirror.Of[T]): Schema[T] = {
    val chez = inline m match
      case p: Mirror.ProductOf[T] => deriveProductWithAnnotations[T](p)
      case s: Mirror.SumOf[T] => deriveSumWithAnnotations[T](s)
    instance[T](chez)
  }

  /**
   * Derive schema for product types (case classes) - original method
   */
  inline def deriveProduct[T](p: Mirror.ProductOf[T]): Chez = {
    val elemLabels = getElemLabels[p.MirroredElemLabels]
    val elemSchemas = getElemSchemas[T, p.MirroredElemTypes]
    val properties = elemLabels.zip(elemSchemas).toMap
    val required = getRequiredFields[p.MirroredElemTypes](elemLabels)

    chez.Chez.Object(
      properties = properties,
      required = required
    )
  }

  /**
   * Derive schema for product types (case classes) with annotation processing
   */
  inline def deriveProductWithAnnotations[T](p: Mirror.ProductOf[T]): Chez = {
    val elemLabels = getElemLabels[p.MirroredElemLabels]

    // Extract field annotations for all fields
    val fieldAnnotations = AnnotationProcessor.extractAllFieldAnnotations[T]

    // Generate schemas for each element with annotations applied
    val elemSchemasWithAnnotations = elemLabels.zip(getElemSchemas[T, p.MirroredElemTypes]).map {
      case (label, schema) =>
        fieldAnnotations.get(label) match {
          case Some(metadata) if metadata.nonEmpty =>
            AnnotationProcessor.applyMetadata(schema, metadata)
          case _ => schema
        }
    }

    val properties = elemLabels.zip(elemSchemasWithAnnotations).toMap
    val required =
      getRequiredFieldsWithDefaults[T, p.MirroredElemTypes](elemLabels, fieldAnnotations)

    // Create base object schema
    val baseObjectSchema = chez.Chez.Object(
      properties = properties,
      required = required
    )

    // Apply class-level annotations
    val classMetadata = AnnotationProcessor.extractClassAnnotations[T]
    val enhancedSchema = AnnotationProcessor.applyMetadata(baseObjectSchema, classMetadata)

    enhancedSchema
  }

  /**
   * Derive schema for sum types (enums/sealed traits)
   */
  inline def deriveSum[T](s: Mirror.SumOf[T]): Chez = {
    val elemLabels = getElemLabels[s.MirroredElemLabels]

    // For Scala 3 enums, we detect them by checking if all element types are singletons
    // If so, generate a string enum schema instead of trying to derive schemas for each case
    inline if (isSimpleEnum[s.MirroredElemTypes]) {
      // Generate enum schema for simple Scala 3 enums using EnumChez
      chez.Chez.StringEnum(elemLabels)
    } else {
      // For complex sum types, derive schemas and use OneOf
      val elemSchemas = getElemSchemas[T, s.MirroredElemTypes]
      if elemSchemas.nonEmpty then
        chez.Chez.OneOf(elemSchemas*)
      else
        chez.Chez.String()
    }
  }

  /**
   * Check if this is a simple enum by examining the element types at compile time
   * Simple enums have singleton cases with empty parameter lists
   * Sealed traits have case classes with non-empty parameter lists
   */
  inline def isSimpleEnum[Elems <: Tuple]: Boolean = {
    inline erasedValue[Elems] match
      case _: (head *: tail) =>
        // Use a different approach: check if this type can be treated as a simple singleton
        // by trying to examine its structure more directly
        inline if (isEmptyProductType[head]) {
          isSimpleEnum[tail] // Continue checking if this is an empty product (enum case)
        } else {
          false // Has fields, so not a simple enum
        }
      case _: EmptyTuple => true
  }

  /**
   * Check if a type is an empty product type (no constructor parameters)
   * This distinguishes enum cases from case classes with parameters
   */
  inline def isEmptyProductType[T]: Boolean = {
    summonFrom {
      case mirror: Mirror.ProductOf[T] =>
        // Check if the mirrored element types is EmptyTuple
        inline erasedValue[mirror.MirroredElemTypes] match
          case _: EmptyTuple => true // No parameters = enum case
          case _ => false // Has parameters = case class
      case _ =>
        // If no Mirror.ProductOf, treat as enum case (true singleton)
        true
    }
  }

  /**
   * Derive schema for sum types (enums/sealed traits) with annotation processing
   */
  inline def deriveSumWithAnnotations[T](s: Mirror.SumOf[T]): Chez = {
    val elemLabels = getElemLabels[s.MirroredElemLabels]

    // For Scala 3 enums, we detect them by checking if all element types are singletons
    // If so, generate a string enum schema instead of trying to derive schemas for each case
    inline if (isSimpleEnum[s.MirroredElemTypes]) {
      // Generate enum schema for simple Scala 3 enums using EnumChez
      chez.Chez.StringEnum(elemLabels)
    } else {
      // For sealed traits, derive schemas with discriminator field injection
      val elemSchemas = getElemSchemas[T, s.MirroredElemTypes]
      if elemSchemas.nonEmpty then
        // Add type discriminator field to each schema variant
        val discriminatedSchemas = elemLabels.zip(elemSchemas).map { case (typeName, schema) =>
          addTypeDiscriminator(schema, typeName)
        }
        chez.Chez.OneOf(discriminatedSchemas*)
      else {
        chez.Chez.String()
      }
    }
  }

  /**
   * Add a type discriminator field to a schema for sealed trait variants
   */
  def addTypeDiscriminator(schema: Chez, typeName: String): Chez = {
    schema match {
      case obj: chez.complex.ObjectChez =>
        // Add "type" field with the variant name as a constant
        val typeField = chez.Chez.String(const = Some(typeName))
        val updatedProperties = obj.properties + ("type" -> typeField)
        val updatedRequired = obj.required + "type"

        obj.copy(
          properties = updatedProperties,
          required = updatedRequired
        )
      case _ =>
        // For non-object schemas, wrap in an object with just the type field
        chez.Chez.Object(
          properties = Map("type" -> chez.Chez.String(const = Some(typeName))),
          required = Set("type")
        )
    }
  }

  /**
   * Get element labels as List[String]
   */
  inline def getElemLabels[Labels <: Tuple]: List[String] = {
    inline erasedValue[Labels] match
      case _: (head *: tail) =>
        constValue[head].asInstanceOf[String] :: getElemLabels[tail]
      case _: EmptyTuple => Nil
  }

  /**
   * Get element schemas by recursively deriving or summoning Schema instances
   */
  inline def getElemSchemas[T, Elems <: Tuple]: List[Chez] = {
    inline erasedValue[Elems] match
      case _: (head *: tail) =>
        getElemSchema[T, head] :: getElemSchemas[T, tail]
      case _: EmptyTuple => Nil
  }

  /**
   * Get schema for a single element type
   */
  inline def getElemSchema[T, Elem]: Chez = {
    inline erasedValue[Elem] match
      case _: String => chez.Chez.String()
      case _: Int => chez.Chez.Integer()
      case _: Long => chez.Chez.Integer()
      case _: Double => chez.Chez.Number()
      case _: Float => chez.Chez.Number()
      case _: Boolean => chez.Chez.Boolean()
      case _: Option[t] => getElemSchema[T, t].optional
      case _: List[t] => chez.Chez.Array(getElemSchema[T, t])
      case _ =>
        // For complex types, try to summon a Schema instance
        summonInline[Schema[Elem]].schema
  }

  /**
   * Determine required fields (non-Option types)
   */
  inline def getRequiredFields[Elems <: Tuple](labels: List[String]): Set[String] = {
    inline erasedValue[Elems] match
      case _: (head *: tail) =>
        val requiredTail = getRequiredFields[tail](labels.tail)
        inline erasedValue[head] match
          case _: Option[_] => requiredTail
          case _ => requiredTail + labels.head
      case _: EmptyTuple => Set.empty
  }

  /**
   * Determine required fields considering both Option types, @Schema.default annotations and Scala defaults
   * Fields are required if they are:
   * 1. Not Option[_] types AND
   * 2. Do not have @Schema.default annotations AND
   * 3. Do not have Scala case class default parameters
   */
  inline def getRequiredFieldsWithDefaults[T, Elems <: Tuple](
      labels: List[String],
      fieldAnnotations: Map[String, AnnotationProcessor.AnnotationMetadata]
  ): Set[String] =
    getRequiredFieldsWithDefaultsHelper[T, Elems](labels, fieldAnnotations, Set.empty, 0)

  /**
   * Helper function to determine required fields recursively
   */
  inline def getRequiredFieldsWithDefaultsHelper[T, Elems <: Tuple](
      labels: List[String],
      fieldAnnotations: Map[String, AnnotationProcessor.AnnotationMetadata],
      acc: Set[String],
      fieldIndex: Int
  ): Set[String] = {
    inline erasedValue[Elems] match
      case _: (head *: tail) =>
        val fieldName = labels.head
        val newAcc = inline erasedValue[head] match
          case _: Option[_] =>
            // Optional fields are never required
            acc
          case _ =>
            // Check if field has @Schema.default annotation
            val hasAnnotationDefault = fieldAnnotations.get(fieldName).exists(_.default.isDefined)

            // Check if field has Scala case class default parameter
            val hasScalaDefault = hasScalaDefaultValue[T](fieldIndex)

            if (hasAnnotationDefault || hasScalaDefault) {
              acc
            } else {
              acc + fieldName
            }

        getRequiredFieldsWithDefaultsHelper[T, tail](
          labels.tail,
          fieldAnnotations,
          newAcc,
          fieldIndex + 1
        )
      case _: EmptyTuple => acc
  }

  /**
   * Check if a field at the given index has a default value in the case class definition
   * Uses compile-time reflection to detect Scala default parameters
   */
  inline def hasScalaDefaultValue[T](fieldIndex: Int): Boolean =
    ${ hasScalaDefaultValueImpl[T]('fieldIndex) }

  /**
   * Macro implementation to detect default values at compile time using field index
   */
  def hasScalaDefaultValueImpl[T: Type](fieldIndexExpr: Expr[Int])(using Quotes): Expr[Boolean] = {
    import quotes.reflect.*

    val fieldIndex = fieldIndexExpr.valueOrAbort
    val tpe = TypeRepr.of[T]

    try {
      // Get the companion object
      val companionSym = tpe.typeSymbol.companionModule
      if (companionSym == Symbol.noSymbol) {
        Expr(false)
      } else {
        // Default methods are named $lessinit$greater$default$N (1-indexed)
        val defaultMethodName = s"$$lessinit$$greater$$default$$${fieldIndex + 1}"

        // Check if the companion object has the default method
        val companionType = companionSym.termRef
        val hasDefaultMethod = companionType.typeSymbol.declaredMethod(defaultMethodName).nonEmpty

        Expr(hasDefaultMethod)
      }
    } catch {
      case _ =>
        // If any reflection fails, assume no default
        Expr(false)
    }
  }

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
  def derived[T](using s: Schema[T], rw: ReadWriter[T]): ReadWriter[T] = {
    readwriter[ujson.Value].bimap[T](
      // Writer: T -> ujson.Value (use case class ReadWriter)
      value => writeJs(value),
      // Reader: ujson.Value -> T (validate against schema, then deserialize)
      json => {
        // Validate against the derived schema
        val validationResult = s.schema.validate(json, ValidationContext())

        if (validationResult.isValid) {
          // Validation passed, deserialize with the case class ReadWriter
          read[T](json)
        } else {
          // Validation failed, create a meaningful error message
          val errorMessages = validationResult.errors.map(_.toString).mkString(", ")
          // scalafix:off DisableSyntax.throw
          // Disabling because throwing an exception is the appropriate way to handle schema validation
          // failures in the default validator implementation - users can override with custom validators
          throw new IllegalArgumentException(s"Schema validation failed: $errorMessages")
          // scalafix:on DisableSyntax.throw
        }
      }
    )
  }
}
