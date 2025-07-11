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
      enumValues: Option[List[String]] = None,
      const: Option[String] = None,
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
    
    var titleOpt: Option[String] = None
    var descriptionOpt: Option[String] = None
    
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
  def extractFieldAnnotationsImpl[T: Type](fieldName: Expr[String])(using Quotes): Expr[AnnotationMetadata] = {
    import quotes.reflect.*
    
    val typeRepr = TypeRepr.of[T]
    val typeSymbol = typeRepr.typeSymbol
    
    fieldName.value match {
      case Some(name) =>
        // Find the field by name
        typeSymbol.primaryConstructor.paramSymss.flatten.find(_.name == name) match {
          case Some(fieldSymbol) =>
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
  def extractAllFieldAnnotationsImpl[T: Type](using Quotes): Expr[Map[String, AnnotationMetadata]] = {
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
  private def extractFieldAnnotationsForSymbol(using Quotes)(fieldSymbol: quotes.reflect.Symbol): Expr[AnnotationMetadata] = {
    import quotes.reflect.*
    
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
        default = $defaultExpr
      )
    }
  }

  /**
   * Apply metadata to enhance a Chez schema
   */
  def applyMetadata(chez: Chez, metadata: AnnotationMetadata): Chez = {
    if (metadata.isEmpty) return chez

    // Apply format-specific metadata FIRST, before wrapping with general metadata
    var result = chez match {
      case sc: StringChez =>
        var enhanced = sc
        metadata.format.foreach(f => enhanced = enhanced.copy(format = Some(f)))
        metadata.minLength.foreach(ml => enhanced = enhanced.copy(minLength = Some(ml)))
        metadata.maxLength.foreach(ml => enhanced = enhanced.copy(maxLength = Some(ml)))
        metadata.pattern.foreach(p => enhanced = enhanced.copy(pattern = Some(p)))
        metadata.const.foreach(c => enhanced = enhanced.copy(const = Some(c)))
        enhanced

      case nc: NumberChez =>
        var enhanced = nc
        metadata.minimum.foreach(min => enhanced = enhanced.copy(minimum = Some(min)))
        metadata.maximum.foreach(max => enhanced = enhanced.copy(maximum = Some(max)))
        metadata.exclusiveMinimum.foreach(emin => enhanced = enhanced.copy(exclusiveMinimum = Some(emin)))
        metadata.exclusiveMaximum.foreach(emax => enhanced = enhanced.copy(exclusiveMaximum = Some(emax)))
        metadata.multipleOf.foreach(mult => enhanced = enhanced.copy(multipleOf = Some(mult)))
        enhanced

      case ic: IntegerChez =>
        var enhanced = ic
        metadata.minimum.foreach(min => enhanced = enhanced.copy(minimum = Some(min.toInt)))
        metadata.maximum.foreach(max => enhanced = enhanced.copy(maximum = Some(max.toInt)))
        metadata.exclusiveMinimum.foreach(emin => enhanced = enhanced.copy(exclusiveMinimum = Some(emin.toInt)))
        metadata.exclusiveMaximum.foreach(emax => enhanced = enhanced.copy(exclusiveMaximum = Some(emax.toInt)))
        metadata.multipleOf.foreach(mult => enhanced = enhanced.copy(multipleOf = Some(mult.toInt)))
        enhanced

      case ac: ArrayChez[_] =>
        var enhanced = ac
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
