package chez.derivation

import scala.quoted.*
import scala.compiletime.*
import chez.*
import chez.primitives.*
import chez.complex.*

/**
 * Production annotation processor with real macro-based annotation extraction
 *
 * This provides complete Phase 4.1 functionality by reading actual @Schema annotations
 * at compile time and applying them to derived schemas.
 */
object AnnotationProcessor {

  /**
   * Metadata extracted from annotations
   */
  case class AnnotationMetadata(
      title: Option[String] = None,
      description: Option[String] = None,
      format: Option[String] = None,
      minLength: Option[Int] = None,
      maxLength: Option[Int] = None,
      minimum: Option[Double] = None,
      maximum: Option[Double] = None,
      pattern: Option[String] = None,
      minItems: Option[Int] = None,
      maxItems: Option[Int] = None,
      uniqueItems: Option[Boolean] = None,
      multipleOf: Option[Double] = None,
      exclusiveMinimum: Option[Double] = None,
      exclusiveMaximum: Option[Double] = None,
      enumValues: Option[List[String | Int | Boolean | Double | Null]] = None,
      const: Option[String | Int | Boolean | Double | Null] = None,
      default: Option[String | Int | Boolean | Double] = None,
      examples: Option[List[String]] = None,
      readOnly: Option[Boolean] = None,
      writeOnly: Option[Boolean] = None,
      deprecated: Option[Boolean] = None
  ) {
    def isEmpty: Boolean = this == AnnotationMetadata()
    def nonEmpty: Boolean = !isEmpty
  }

  /**
   * Extract class-level annotations at compile time using macros
   */
  inline def extractClassAnnotations[T]: AnnotationMetadata = {
    ${ extractClassAnnotationsImpl[T] }
  }

  /**
   * Extract field-level annotations at compile time using macros
   */
  inline def extractFieldAnnotations[T](fieldName: String): AnnotationMetadata = {
    ${ extractFieldAnnotationsImpl[T]('fieldName) }
  }

  /**
   * Extract all field annotations for a type using macros
   */
  inline def extractAllFieldAnnotations[T]: Map[String, AnnotationMetadata] = {
    ${ extractAllFieldAnnotationsImpl[T] }
  }

  /**
   * Macro implementation for extracting class-level annotations
   */
  def extractClassAnnotationsImpl[T: Type](using Quotes): Expr[AnnotationMetadata] = {
    import quotes.reflect.*

    val typeRepr = TypeRepr.of[T]
    val typeSymbol = typeRepr.typeSymbol

    // scalafix:off DisableSyntax.var
    // Disabling because macros require mutable variables to accumulate annotation values
    // as we iterate through the annotation tree at compile time
    var titleOpt: Option[String] = None
    var descriptionOpt: Option[String] = None
    // scalafix:on DisableSyntax.var

    // Extract annotations from the class
    for (annotation <- typeSymbol.annotations) {
      annotation.tpe.typeSymbol.name match {
        case "title" =>
          annotation match {
            case Apply(_, List(Literal(StringConstant(value)))) =>
              titleOpt = Some(value)
            case _ =>
          }
        case "description" =>
          annotation match {
            case Apply(_, List(Literal(StringConstant(value)))) =>
              descriptionOpt = Some(value)
            case _ =>
          }
        case _ =>
      }
    }

    // Build AnnotationMetadata expression
    val titleExpr = titleOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val descriptionExpr = descriptionOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }

    '{ AnnotationMetadata(title = $titleExpr, description = $descriptionExpr) }
  }

  /**
   * Macro implementation for extracting field-level annotations
   */
  def extractFieldAnnotationsImpl[T: Type](fieldName: Expr[String])(using
      Quotes
  ): Expr[AnnotationMetadata] = {
    import quotes.reflect.*

    val typeRepr = TypeRepr.of[T]
    val typeSymbol = typeRepr.typeSymbol

    fieldName.value match {
      case Some(name) =>
        // Find the field by name
        typeSymbol.primaryConstructor.paramSymss.flatten.find(_.name == name) match {
          case Some(fieldSymbol) =>
            // scalafix:off DisableSyntax.var
            // Disabling because macros require mutable variables to accumulate annotation values
            // as we iterate through field annotations at compile time
            var descriptionOpt: Option[String] = None
            var formatOpt: Option[String] = None
            var minLengthOpt: Option[Int] = None
            var maxLengthOpt: Option[Int] = None
            var minimumOpt: Option[Double] = None
            var maximumOpt: Option[Double] = None
            var patternOpt: Option[String] = None
            var minItemsOpt: Option[Int] = None
            var maxItemsOpt: Option[Int] = None
            var uniqueItemsOpt: Option[Boolean] = None
            var defaultOpt: Option[String | Int | Boolean | Double] = None
            var enumValuesOpt: Option[List[String | Int | Boolean | Double | Null]] = None
            var constOpt: Option[String | Int | Boolean | Double | Null] = None
            // scalafix:on DisableSyntax.var

            // Extract annotations from the field
            for (annotation <- fieldSymbol.annotations) {
              annotation.tpe.typeSymbol.name match {
                case "description" =>
                  annotation match {
                    case Apply(_, List(Literal(StringConstant(value)))) =>
                      descriptionOpt = Some(value)
                    case _ =>
                  }
                case "format" =>
                  annotation match {
                    case Apply(_, List(Literal(StringConstant(value)))) =>
                      formatOpt = Some(value)
                    case _ =>
                  }
                case "minLength" =>
                  annotation match {
                    case Apply(_, List(Literal(IntConstant(value)))) =>
                      minLengthOpt = Some(value)
                    case _ =>
                  }
                case "maxLength" =>
                  annotation match {
                    case Apply(_, List(Literal(IntConstant(value)))) =>
                      maxLengthOpt = Some(value)
                    case _ =>
                  }
                case "minimum" =>
                  annotation match {
                    case Apply(_, List(Literal(DoubleConstant(value)))) =>
                      minimumOpt = Some(value)
                    case _ =>
                  }
                case "maximum" =>
                  annotation match {
                    case Apply(_, List(Literal(DoubleConstant(value)))) =>
                      maximumOpt = Some(value)
                    case _ =>
                  }
                case "pattern" =>
                  annotation match {
                    case Apply(_, List(Literal(StringConstant(value)))) =>
                      patternOpt = Some(value)
                    case _ =>
                  }
                case "minItems" =>
                  annotation match {
                    case Apply(_, List(Literal(IntConstant(value)))) =>
                      minItemsOpt = Some(value)
                    case _ =>
                  }
                case "maxItems" =>
                  annotation match {
                    case Apply(_, List(Literal(IntConstant(value)))) =>
                      maxItemsOpt = Some(value)
                    case _ =>
                  }
                case "uniqueItems" =>
                  annotation match {
                    case Apply(_, List(Literal(BooleanConstant(value)))) =>
                      uniqueItemsOpt = Some(value)
                    case Apply(_, Nil) => // Default true for uniqueItems
                      uniqueItemsOpt = Some(true)
                    case _ =>
                  }
                case "enumValues" =>
                  annotation match {
                    case Apply(_, args) =>
                      val values = args.collect {
                        case Literal(StringConstant(value)) =>
                          value: String | Int | Boolean | Double | Null
                        case Literal(IntConstant(value)) =>
                          value: String | Int | Boolean | Double | Null
                        case Literal(BooleanConstant(value)) =>
                          value: String | Int | Boolean | Double | Null
                        case Literal(DoubleConstant(value)) =>
                          value: String | Int | Boolean | Double | Null
                        // scalafix:off DisableSyntax.null
                        // Disabling because we need to handle null as a valid enum value according to JSON Schema spec
                        case Literal(NullConstant()) => null: String | Int | Boolean | Double | Null
                        // scalafix:on DisableSyntax.null
                      }
                      if (values.nonEmpty) enumValuesOpt = Some(values)
                    case _ =>
                  }
                case "const" =>
                  annotation match {
                    case Apply(_, List(Literal(StringConstant(value)))) =>
                      constOpt = Some(value)
                    case Apply(_, List(Literal(IntConstant(value)))) =>
                      constOpt = Some(value)
                    case Apply(_, List(Literal(BooleanConstant(value)))) =>
                      constOpt = Some(value)
                    case Apply(_, List(Literal(DoubleConstant(value)))) =>
                      constOpt = Some(value)
                    case Apply(_, List(Literal(NullConstant()))) =>
                      // scalafix:off DisableSyntax.null
                      // Disabling because null is a valid const value in JSON Schema
                      constOpt = Some(null)
                    // scalafix:on DisableSyntax.null
                    case _ =>
                  }
                case "default" =>
                  annotation match {
                    case Apply(_, List(Literal(StringConstant(value)))) =>
                      defaultOpt = Some(value)
                    case Apply(_, List(Literal(IntConstant(value)))) =>
                      defaultOpt = Some(value)
                    case Apply(_, List(Literal(BooleanConstant(value)))) =>
                      defaultOpt = Some(value)
                    case Apply(_, List(Literal(DoubleConstant(value)))) =>
                      defaultOpt = Some(value)
                    case _ =>
                  }
                case _ =>
              }
            }

            // Build expressions for all Option fields
            val descriptionExpr = descriptionOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val formatExpr = formatOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val minLengthExpr = minLengthOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val maxLengthExpr = maxLengthOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val minimumExpr = minimumOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val maximumExpr = maximumOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val patternExpr = patternOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val minItemsExpr = minItemsOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val maxItemsExpr = maxItemsOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val uniqueItemsExpr = uniqueItemsOpt match {
              case Some(value) => '{ Some(${ Expr(value) }) }
              case None => '{ None }
            }
            val enumValuesExpr = enumValuesOpt match {
              case Some(values) =>
                val valueExprs = values.map {
                  case s: String => '{ ${ Expr(s) }: String | Int | Boolean | Double | Null }
                  case i: Int => '{ ${ Expr(i) }: String | Int | Boolean | Double | Null }
                  case b: Boolean => '{ ${ Expr(b) }: String | Int | Boolean | Double | Null }
                  case d: Double => '{ ${ Expr(d) }: String | Int | Boolean | Double | Null }
                  // scalafix:off DisableSyntax.null
                  // Disabling because we're building expressions for null values in enum lists
                  case null => '{ null: String | Int | Boolean | Double | Null }
                  // scalafix:on DisableSyntax.null
                }
                '{ Some(List(${ Expr.ofSeq(valueExprs) }*)) }
              case None => '{ None }
            }
            val constExpr = constOpt match {
              case Some(value: String) =>
                '{ Some(${ Expr(value) }: String | Int | Boolean | Double | Null) }
              case Some(value: Int) =>
                '{ Some(${ Expr(value) }: String | Int | Boolean | Double | Null) }
              case Some(value: Boolean) =>
                '{ Some(${ Expr(value) }: String | Int | Boolean | Double | Null) }
              case Some(value: Double) =>
                '{ Some(${ Expr(value) }: String | Int | Boolean | Double | Null) }
              // scalafix:off DisableSyntax.null
              // Disabling because we're building expressions for null const values
              case Some(null) => '{ Some(null: String | Int | Boolean | Double | Null) }
              // scalafix:on DisableSyntax.null
              case None => '{ None }
            }
            val defaultExpr = defaultOpt match {
              case Some(value: String) =>
                '{ Some(${ Expr(value) }: String | Int | Boolean | Double) }
              case Some(value: Int) => '{ Some(${ Expr(value) }: String | Int | Boolean | Double) }
              case Some(value: Boolean) =>
                '{ Some(${ Expr(value) }: String | Int | Boolean | Double) }
              case Some(value: Double) =>
                '{ Some(${ Expr(value) }: String | Int | Boolean | Double) }
              case None => '{ None }
            }

            '{
              AnnotationMetadata(
                description = $descriptionExpr,
                format = $formatExpr,
                minLength = $minLengthExpr,
                maxLength = $maxLengthExpr,
                minimum = $minimumExpr,
                maximum = $maximumExpr,
                pattern = $patternExpr,
                minItems = $minItemsExpr,
                maxItems = $maxItemsExpr,
                uniqueItems = $uniqueItemsExpr,
                enumValues = $enumValuesExpr,
                const = $constExpr,
                default = $defaultExpr
              )
            }
          case None =>
            '{ AnnotationMetadata() }
        }
      case None =>
        '{ AnnotationMetadata() }
    }
  }

  /**
   * Macro implementation for extracting all field annotations
   */
  def extractAllFieldAnnotationsImpl[T: Type](using
      Quotes
  ): Expr[Map[String, AnnotationMetadata]] = {
    import quotes.reflect.*

    val typeRepr = TypeRepr.of[T]
    val typeSymbol = typeRepr.typeSymbol

    val fieldExprs = typeSymbol.primaryConstructor.paramSymss.flatten.map { fieldSymbol =>
      val fieldName = fieldSymbol.name
      val fieldNameExpr = Expr(fieldName)
      val fieldMetadataExpr = extractFieldAnnotationsForSymbol(fieldSymbol)
      '{ ($fieldNameExpr, $fieldMetadataExpr) }
    }

    // Build map from field expressions
    val pairs = fieldExprs.map { pairExpr =>
      '{ List($pairExpr) }
    }.reduceLeftOption { (acc, curr) =>
      '{ $acc ++ $curr }
    }.getOrElse('{ List.empty[(String, AnnotationMetadata)] })

    '{ $pairs.toMap }
  }

  /**
   * Helper method to extract annotations from a field symbol
   */
  private def extractFieldAnnotationsForSymbol(using
      Quotes
  )(fieldSymbol: quotes.reflect.Symbol): Expr[AnnotationMetadata] = {
    import quotes.reflect.*

    // scalafix:off DisableSyntax.var
    // Disabling because macros require mutable variables to accumulate field annotation values
    // during compile-time tree traversal
    var descriptionOpt: Option[String] = None
    var formatOpt: Option[String] = None
    var minLengthOpt: Option[Int] = None
    var maxLengthOpt: Option[Int] = None
    var minimumOpt: Option[Double] = None
    var maximumOpt: Option[Double] = None
    var patternOpt: Option[String] = None
    var minItemsOpt: Option[Int] = None
    var maxItemsOpt: Option[Int] = None
    var uniqueItemsOpt: Option[Boolean] = None
    var defaultOpt: Option[String | Int | Boolean | Double] = None
    var enumValuesOpt: Option[List[String | Int | Boolean | Double | Null]] = None
    var constOpt: Option[String | Int | Boolean | Double | Null] = None
    // scalafix:on DisableSyntax.var

    // Extract annotations from the field
    for (annotation <- fieldSymbol.annotations) {
      annotation.tpe.typeSymbol.name match {
        case "description" =>
          annotation match {
            case Apply(_, List(Literal(StringConstant(value)))) =>
              descriptionOpt = Some(value)
            case _ =>
          }
        case "format" =>
          annotation match {
            case Apply(_, List(Literal(StringConstant(value)))) =>
              formatOpt = Some(value)
            case _ =>
          }
        case "minLength" =>
          annotation match {
            case Apply(_, List(Literal(IntConstant(value)))) =>
              minLengthOpt = Some(value)
            case _ =>
          }
        case "maxLength" =>
          annotation match {
            case Apply(_, List(Literal(IntConstant(value)))) =>
              maxLengthOpt = Some(value)
            case _ =>
          }
        case "minimum" =>
          annotation match {
            case Apply(_, List(Literal(DoubleConstant(value)))) =>
              minimumOpt = Some(value)
            case _ =>
          }
        case "maximum" =>
          annotation match {
            case Apply(_, List(Literal(DoubleConstant(value)))) =>
              maximumOpt = Some(value)
            case _ =>
          }
        case "pattern" =>
          annotation match {
            case Apply(_, List(Literal(StringConstant(value)))) =>
              patternOpt = Some(value)
            case _ =>
          }
        case "minItems" =>
          annotation match {
            case Apply(_, List(Literal(IntConstant(value)))) =>
              minItemsOpt = Some(value)
            case _ =>
          }
        case "maxItems" =>
          annotation match {
            case Apply(_, List(Literal(IntConstant(value)))) =>
              maxItemsOpt = Some(value)
            case _ =>
          }
        case "uniqueItems" =>
          annotation match {
            case Apply(_, List(Literal(BooleanConstant(value)))) =>
              uniqueItemsOpt = Some(value)
            case Apply(_, Nil) => // Default true for uniqueItems
              uniqueItemsOpt = Some(true)
            case _ =>
          }
        case "enumValues" =>
          annotation match {
            case Apply(_, args) =>
              val values = args.collect {
                case Literal(StringConstant(value)) => value: String | Int | Boolean | Double | Null
                case Literal(IntConstant(value)) => value: String | Int | Boolean | Double | Null
                case Literal(BooleanConstant(value)) =>
                  value: String | Int | Boolean | Double | Null
                case Literal(DoubleConstant(value)) => value: String | Int | Boolean | Double | Null
                // scalafix:off DisableSyntax.null
                // Disabling because we need to handle null as a valid enum value
                case Literal(NullConstant()) => null: String | Int | Boolean | Double | Null
                // scalafix:on DisableSyntax.null
              }
              if (values.nonEmpty) enumValuesOpt = Some(values)
            case _ =>
          }
        case "const" =>
          annotation match {
            case Apply(_, List(Literal(StringConstant(value)))) =>
              constOpt = Some(value)
            case Apply(_, List(Literal(IntConstant(value)))) =>
              constOpt = Some(value)
            case Apply(_, List(Literal(BooleanConstant(value)))) =>
              constOpt = Some(value)
            case Apply(_, List(Literal(DoubleConstant(value)))) =>
              constOpt = Some(value)
            case Apply(_, List(Literal(NullConstant()))) =>
              // scalafix:off DisableSyntax.null
              // Disabling because null is a valid const value
              constOpt = Some(null)
            // scalafix:on DisableSyntax.null
            case _ =>
          }
        case "default" =>
          annotation match {
            case Apply(_, List(Literal(StringConstant(value)))) =>
              defaultOpt = Some(value)
            case Apply(_, List(Literal(IntConstant(value)))) =>
              defaultOpt = Some(value)
            case Apply(_, List(Literal(BooleanConstant(value)))) =>
              defaultOpt = Some(value)
            case Apply(_, List(Literal(DoubleConstant(value)))) =>
              defaultOpt = Some(value)
            case _ =>
          }
        case _ =>
      }
    }

    // Build expressions for all Option fields
    val descriptionExpr = descriptionOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val formatExpr = formatOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val minLengthExpr = minLengthOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val maxLengthExpr = maxLengthOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val minimumExpr = minimumOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val maximumExpr = maximumOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val patternExpr = patternOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val minItemsExpr = minItemsOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val maxItemsExpr = maxItemsOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val uniqueItemsExpr = uniqueItemsOpt match {
      case Some(value) => '{ Some(${ Expr(value) }) }
      case None => '{ None }
    }
    val enumValuesExpr = enumValuesOpt match {
      case Some(values) =>
        val valueExprs = values.map {
          case s: String => '{ ${ Expr(s) }: String | Int | Boolean | Double | Null }
          case i: Int => '{ ${ Expr(i) }: String | Int | Boolean | Double | Null }
          case b: Boolean => '{ ${ Expr(b) }: String | Int | Boolean | Double | Null }
          case d: Double => '{ ${ Expr(d) }: String | Int | Boolean | Double | Null }
          // scalafix:off DisableSyntax.null
          // Disabling because we're building expressions for null enum values
          case null => '{ null: String | Int | Boolean | Double | Null }
          // scalafix:on DisableSyntax.null
        }
        '{ Some(List(${ Expr.ofSeq(valueExprs) }*)) }
      case None => '{ None }
    }
    val constExpr = constOpt match {
      case Some(value: String) =>
        '{ Some(${ Expr(value) }: String | Int | Boolean | Double | Null) }
      case Some(value: Int) => '{ Some(${ Expr(value) }: String | Int | Boolean | Double | Null) }
      case Some(value: Boolean) =>
        '{ Some(${ Expr(value) }: String | Int | Boolean | Double | Null) }
      case Some(value: Double) =>
        '{ Some(${ Expr(value) }: String | Int | Boolean | Double | Null) }
      // scalafix:off DisableSyntax.null
      // Disabling because we're building expressions for null const values
      case Some(null) => '{ Some(null: String | Int | Boolean | Double | Null) }
      // scalafix:on DisableSyntax.null
      case None => '{ None }
    }
    val defaultExpr = defaultOpt match {
      case Some(value: String) => '{ Some(${ Expr(value) }: String | Int | Boolean | Double) }
      case Some(value: Int) => '{ Some(${ Expr(value) }: String | Int | Boolean | Double) }
      case Some(value: Boolean) => '{ Some(${ Expr(value) }: String | Int | Boolean | Double) }
      case Some(value: Double) => '{ Some(${ Expr(value) }: String | Int | Boolean | Double) }
      case None => '{ None }
    }

    '{
      AnnotationMetadata(
        description = $descriptionExpr,
        format = $formatExpr,
        minLength = $minLengthExpr,
        maxLength = $maxLengthExpr,
        minimum = $minimumExpr,
        maximum = $maximumExpr,
        pattern = $patternExpr,
        minItems = $minItemsExpr,
        maxItems = $maxItemsExpr,
        uniqueItems = $uniqueItemsExpr,
        enumValues = $enumValuesExpr,
        const = $constExpr,
        default = $defaultExpr
      )
    }
  }

  /**
   * Apply metadata to enhance a Chez schema
   */
  def applyMetadata(chez: Chez, metadata: AnnotationMetadata): Chez = {
    if (metadata.isEmpty) return chez

    // If enumValues is specified, create an EnumChez instead
    metadata.enumValues match {
      case Some(values) =>
        val ujsonValues = values.map {
          case s: String => ujson.Str(s)
          case i: Int => ujson.Num(i)
          case b: Boolean => ujson.Bool(b)
          case d: Double => ujson.Num(d)
          // scalafix:off DisableSyntax.null
          // Disabling because we need to convert null enum values to ujson.Null
          case null => ujson.Null
          // scalafix:on DisableSyntax.null
        }
        // scalafix:off DisableSyntax.var
        // Disabling because we need to mutate the result as we apply various metadata properties
        var result: Chez = EnumChez(ujsonValues)
        // scalafix:on DisableSyntax.var

        // Apply general metadata to the enum
        metadata.title.foreach(title => result = result.withTitle(title))
        metadata.description.foreach(desc => result = result.withDescription(desc))
        metadata.default.foreach { defaultValue =>
          val ujsonDefault = defaultValue match {
            case s: String => ujson.Str(s)
            case i: Int => ujson.Num(i)
            case b: Boolean => ujson.Bool(b)
            case d: Double => ujson.Num(d)
          }
          result = result.withDefault(ujsonDefault)
        }

        return result
      case None =>
    }

    // Apply format-specific metadata FIRST, before wrapping with general metadata
    // scalafix:off DisableSyntax.var
    // Disabling because we need to mutate the result as we apply various metadata enhancements
    var result = chez match {
      case sc: StringChez =>
        var enhanced = sc
        // scalafix:on DisableSyntax.var
        metadata.format.foreach(f => enhanced = enhanced.copy(format = Some(f)))
        metadata.minLength.foreach(ml => enhanced = enhanced.copy(minLength = Some(ml)))
        metadata.maxLength.foreach(ml => enhanced = enhanced.copy(maxLength = Some(ml)))
        metadata.pattern.foreach(p => enhanced = enhanced.copy(pattern = Some(p)))
        metadata.const.foreach {
          case s: String => enhanced = enhanced.copy(const = Some(s))
          case _ => // Non-string const values don't apply to StringChez
        }
        enhanced

      case nc: NumberChez =>
        // scalafix:off DisableSyntax.var
        // Disabling because we need to mutate enhanced as we apply numeric constraints
        var enhanced = nc
        // scalafix:on DisableSyntax.var
        metadata.minimum.foreach(min => enhanced = enhanced.copy(minimum = Some(min)))
        metadata.maximum.foreach(max => enhanced = enhanced.copy(maximum = Some(max)))
        metadata.exclusiveMinimum.foreach(emin =>
          enhanced = enhanced.copy(exclusiveMinimum = Some(emin))
        )
        metadata.exclusiveMaximum.foreach(emax =>
          enhanced = enhanced.copy(exclusiveMaximum = Some(emax))
        )
        metadata.multipleOf.foreach(mult => enhanced = enhanced.copy(multipleOf = Some(mult)))
        metadata.const.foreach {
          case d: Double => enhanced = enhanced.copy(const = Some(d))
          case i: Int => enhanced = enhanced.copy(const = Some(i.toDouble))
          case _ => // Non-numeric const values don't apply to NumberChez
        }
        enhanced

      case ic: IntegerChez =>
        // scalafix:off DisableSyntax.var
        // Disabling because we need to mutate enhanced as we apply integer constraints
        var enhanced = ic
        // scalafix:on DisableSyntax.var
        metadata.minimum.foreach(min => enhanced = enhanced.copy(minimum = Some(min.toInt)))
        metadata.maximum.foreach(max => enhanced = enhanced.copy(maximum = Some(max.toInt)))
        metadata.exclusiveMinimum.foreach(emin =>
          enhanced = enhanced.copy(exclusiveMinimum = Some(emin.toInt))
        )
        metadata.exclusiveMaximum.foreach(emax =>
          enhanced = enhanced.copy(exclusiveMaximum = Some(emax.toInt))
        )
        metadata.multipleOf.foreach(mult => enhanced = enhanced.copy(multipleOf = Some(mult.toInt)))
        metadata.const.foreach {
          case i: Int => enhanced = enhanced.copy(const = Some(i))
          case d: Double => enhanced = enhanced.copy(const = Some(d.toInt))
          case _ => // Non-numeric const values don't apply to IntegerChez
        }
        enhanced

      case ac: ArrayChez[_] =>
        // scalafix:off DisableSyntax.var
        // Disabling because we need to mutate enhanced as we apply array constraints
        var enhanced = ac
        // scalafix:on DisableSyntax.var
        metadata.minItems.foreach(min => enhanced = enhanced.copy(minItems = Some(min)))
        metadata.maxItems.foreach(max => enhanced = enhanced.copy(maxItems = Some(max)))
        metadata.uniqueItems.foreach(unique => enhanced = enhanced.copy(uniqueItems = Some(unique)))
        enhanced

      case other => other
    }

    // Apply general metadata AFTER type-specific metadata
    metadata.title.foreach(title => result = result.withTitle(title))
    metadata.description.foreach(desc => result = result.withDescription(desc))

    // Apply default values
    metadata.default.foreach { defaultValue =>
      val ujsonDefault = defaultValue match {
        case s: String => ujson.Str(s)
        case i: Int => ujson.Num(i)
        case b: Boolean => ujson.Bool(b)
        case d: Double => ujson.Num(d)
      }
      result = result.withDefault(ujsonDefault)
    }

    // Apply examples if present
    metadata.examples.foreach { examples =>
      val ujsonExamples = examples.flatMap { ex =>
        try {
          Some(ujson.read(ex))
        } catch {
          case _: Exception => None
        }
      }
      if (ujsonExamples.nonEmpty) {
        result = result.withExamples(ujsonExamples*)
      }
    }

    result
  }
}
