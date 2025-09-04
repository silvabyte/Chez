package chez.derivation

import utest.*
import chez.*
import chez.complex.*
 

object SealedTraitDerivationTests extends TestSuite {

  // Basic sealed trait with case classes
  sealed trait Shape derives Schema
  case class Circle(radius: Double) extends Shape derives Schema
  case class Rectangle(width: Double, height: Double) extends Shape derives Schema
  case class Triangle(base: Double, height: Double) extends Shape derives Schema

  // Sealed trait with single case class
  sealed trait SingleVariant derives Schema
  case class OnlyOne(value: String) extends SingleVariant derives Schema

  // Sealed trait with mixed parameter types
  sealed trait MixedData derives Schema
  case class TextData(content: String) extends MixedData derives Schema
  case class NumberData(value: Double, count: Int) extends MixedData derives Schema
  case class BooleanData(flag: Boolean) extends MixedData derives Schema

  // Sealed trait with optional fields
  sealed trait OptionalFields derives Schema
  case class WithOptional(required: String, optional: Option[Int]) extends OptionalFields
      derives Schema
  case class AllRequired(name: String, age: Int) extends OptionalFields derives Schema

  // Nested sealed traits
  sealed trait NestedParent derives Schema
  case class NestedChild(name: String, data: MixedData) extends NestedParent derives Schema
  case class SimpleChild(id: Int) extends NestedParent derives Schema

  // Empty case class in sealed trait
  sealed trait WithEmpty derives Schema
  case class EmptyCase() extends WithEmpty derives Schema
  case class NonEmptyCase(value: String) extends WithEmpty derives Schema

  val tests = Tests {
    test("Basic sealed trait discriminated union generation") {
      test("Shape sealed trait generates oneOf schema") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema

        // Should use oneOf, not enum
        assert(json.obj.contains("oneOf"))
        assert(!json.obj.contains("enum"))
        assert(!json.obj.contains("type"))

        val oneOfArray = json("oneOf").arr
        assert(oneOfArray.length == 3) // Circle, Rectangle, Triangle
      }

      test("Shape variants have correct object structure") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        // All variants should be objects
        variants.foreach { variant =>
          assert(variant("type").str == "object")
          assert(variant.obj.contains("properties"))
          assert(variant.obj.contains("required"))
        }
      }

      test("Single variant sealed trait works correctly") {
        val schema = Schema[SingleVariant]
        val json = schema.toJsonSchema

        assert(json.obj.contains("oneOf"))
        val variants = json("oneOf").arr
        assert(variants.length == 1)

        val variant = variants(0)
        assert(variant("type").str == "object")
        assert(variant("properties").obj.contains("value"))
        assert(variant("properties").obj.contains("type"))
      }
    }

    test("Type discriminator field injection") {
      test("Circle has correct type discriminator") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        // Find Circle variant (should have radius property)
        val circleVariant = variants.find(_.obj.contains("properties") &&
          variants.find(v => v("properties").obj.contains("radius")).isDefined).get

        val props = circleVariant("properties")
        assert(props.obj.contains("type"))

        val typeField = props("type")
        assert(typeField("type").str == "string")
        assert(typeField.obj.contains("const"))
        assert(typeField("const").str == "Circle")
      }

      test("Rectangle has correct type discriminator") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        // Find Rectangle variant (should have width and height properties)
        val rectangleVariant = variants.find { v =>
          v.obj.contains("properties") && {
            val props = v("properties").obj
            props.contains("width") && props.contains("height")
          }
        }.get

        val props = rectangleVariant("properties")
        val typeField = props("type")
        assert(typeField("type").str == "string")
        assert(typeField("const").str == "Rectangle")
      }

      test("Type field is required in all variants") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        variants.foreach { variant =>
          val required = variant("required").arr.map(_.str).toSet
          assert(required.contains("type"))
        }
      }

      test("Empty case class gets type discriminator") {
        val schema = Schema[WithEmpty]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        // Find EmptyCase variant
        val emptyVariant = variants.find { v =>
          val required = v("required").arr.map(_.str).toSet
          required == Set("type") // Only type field required
        }.get

        val props = emptyVariant("properties")
        assert(props.obj.size == 1) // Only type field
        assert(props.obj.contains("type"))
        assert(props("type")("const").str == "EmptyCase")
      }
    }

    test("Sealed trait schema structure validation") {
      test("Sealed trait generates oneOf, not string enum") {
        val sealedSchema = Schema[Shape]
        val sealedJson = sealedSchema.toJsonSchema

        // Should be oneOf, not string enum
        assert(sealedJson.obj.contains("oneOf"))
        assert(!sealedJson.obj.contains("enum"))
        assert(!sealedJson.obj.contains("type"))
      }

      test("Sealed trait discriminated union structure") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema

        // Should have oneOf at root level
        assert(json.obj.contains("oneOf"))
        assert(json.obj.size == 1) // Only oneOf, no other properties

        val variants = json("oneOf").arr
        variants.foreach { variant =>
          // Each variant should be a valid object schema
          assert(variant("type").str == "object")
          assert(variant.obj.contains("properties"))
          assert(variant.obj.contains("required"))
        }
      }
    }

    test("Complex sealed trait scenarios") {
      test("Mixed parameter types work correctly") {
        val schema = Schema[MixedData]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        assert(variants.length == 3) // TextData, NumberData, BooleanData

        // Find TextData variant
        val textVariant = variants.find { v =>
          val props = v("properties").obj
          props.contains("content") && props("type")("const").str == "TextData"
        }.get

        val textProps = textVariant("properties")
        assert(textProps("content")("type").str == "string")
        assert(textProps("type")("const").str == "TextData")

        // Find NumberData variant
        val numberVariant = variants.find { v =>
          val props = v("properties").obj
          props.contains("value") && props.contains("count") && props("type")(
            "const"
          ).str == "NumberData"
        }.get

        val numberProps = numberVariant("properties")
        assert(numberProps("value")("type").str == "number")
        assert(numberProps("count")("type").str == "integer")
        assert(numberProps("type")("const").str == "NumberData")
      }

      test("Optional fields are handled correctly") {
        val schema = Schema[OptionalFields]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        // Find WithOptional variant
        val optionalVariant = variants.find { v =>
          val props = v("properties").obj
          props.contains("optional") && props("type")("const").str == "WithOptional"
        }.get

        val required = optionalVariant("required").arr.map(_.str).toSet
        assert(required.contains("required")) // required field
        assert(required.contains("type")) // type discriminator
        assert(!required.contains("optional")) // optional field not required

        val props = optionalVariant("properties")
        assert(props.obj.contains("optional")) // optional field exists in properties
        assert(props("optional")("type").str == "integer") // but is optional
      }

      test("Nested sealed traits work correctly") {
        val schema = Schema[NestedParent]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        // Find NestedChild variant
        val nestedVariant = variants.find { v =>
          val props = v("properties").obj
          props.contains("data") && props("type")("const").str == "NestedChild"
        }.get

        val props = nestedVariant("properties")
        assert(props.obj.contains("name"))
        assert(props.obj.contains("data"))
        assert(props.obj.contains("type"))

        // The data field should reference the nested sealed trait schema
        val dataField = props("data")
        assert(dataField.obj.contains("oneOf")) // Nested sealed trait
      }
    }

    test("Schema property validation") {
      test("All variants maintain property types") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        variants.foreach { variant =>
          val props = variant("properties")

          // All should have type discriminator
          assert(props.obj.contains("type"))
          val typeField = props("type")
          assert(typeField("type").str == "string")
          assert(typeField.obj.contains("const"))

          // Check specific variant properties
          if (props.obj.contains("radius")) {
            // Circle
            assert(props("radius")("type").str == "number")
            assert(typeField("const").str == "Circle")
          } else if (props.obj.contains("width")) {
            // Rectangle
            assert(props("width")("type").str == "number")
            assert(props("height")("type").str == "number")
            assert(typeField("const").str == "Rectangle")
          } else if (props.obj.contains("base")) {
            // Triangle
            assert(props("base")("type").str == "number")
            assert(props("height")("type").str == "number")
            assert(typeField("const").str == "Triangle")
          }
        }
      }

      test("Required fields include original fields plus type") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        variants.foreach { variant =>
          val required = variant("required").arr.map(_.str).toSet
          val props = variant("properties").obj.keys.toSet

          // Type field should always be required
          assert(required.contains("type"))

          // All non-type properties should be required (for this test case)
          val nonTypeProps = props - "type"
          nonTypeProps.foreach { prop =>
            assert(required.contains(prop))
          }
        }
      }
    }

    test("Integration with case classes") {
      test("Case class containing sealed trait field") {
        case class Container(name: String, shape: Shape) derives Schema

        val schema = Schema[Container]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        val props = json("properties")

        assert(props("name")("type").str == "string")
        assert(props.obj.contains("shape"))

        // Shape field should use the sealed trait schema
        val shapeField = props("shape")
        assert(shapeField.obj.contains("oneOf"))

        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name", "shape"))
      }

      test("Optional sealed trait field") {
        case class OptionalContainer(name: String, shape: Option[Shape]) derives Schema

        val schema = Schema[OptionalContainer]
        val json = schema.toJsonSchema

        val props = json("properties")
        assert(props.obj.contains("shape"))

        // Shape field should still use sealed trait schema
        val shapeField = props("shape")
        assert(shapeField.obj.contains("oneOf"))

        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name")) // shape is optional
      }
    }

    test("Edge cases and error conditions") {
      test("Sealed trait with single variant generates oneOf") {
        val schema = Schema[SingleVariant]
        val json = schema.toJsonSchema

        // Even single variant should use oneOf for consistency
        assert(json.obj.contains("oneOf"))
        val variants = json("oneOf").arr
        assert(variants.length == 1)

        val variant = variants(0)
        assert(variant("properties")("type")("const").str == "OnlyOne")
      }

      test("Empty case class in sealed trait") {
        val schema = Schema[WithEmpty]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        // Find EmptyCase variant
        val emptyVariant = variants.find { v =>
          v("properties")("type")("const").str == "EmptyCase"
        }.get

        // Should only have type property
        val props = emptyVariant("properties").obj
        assert(props.size == 1)
        assert(props.contains("type"))

        val required = emptyVariant("required").arr.map(_.str).toSet
        assert(required == Set("type"))
      }
    }

    test("JSON Schema 2020-12 compliance") {
      test("Discriminator const values are unique") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        val typeValues = variants.map { variant =>
          variant("properties")("type")("const").str
        }.toSet

        assert(typeValues.size == variants.length) // All unique
        assert(typeValues == Set("Circle", "Rectangle", "Triangle"))
      }

      test("Properties have valid JSON Schema structure") {
        val schema = Schema[Shape]
        val json = schema.toJsonSchema
        val variants = json("oneOf").arr

        variants.foreach { variant =>
          // Properties should be valid
          val props = variant("properties").obj
          props.foreach { case (name, propSchema) =>
            assert(propSchema.obj.contains("type"))
            if (name == "type") {
              assert(propSchema.obj.contains("const"))
            }
          }
        }
      }
    }

    test("Individual case class schema derivation") {
      test("Circle case class generates correct object schema") {
        val schema = Schema[Circle]
        val json = schema.toJsonSchema

        // Should be an object schema (not oneOf)
        assert(json("type").str == "object")
        assert(json.obj.contains("properties"))
        assert(json.obj.contains("required"))

        // Check properties
        val props = json("properties")
        assert(props.obj.contains("radius"))
        assert(props("radius")("type").str == "number")

        // Check required fields
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("radius"))

        // Should NOT have type discriminator when used individually
        assert(!props.obj.contains("type"))
      }

      test("Rectangle case class generates correct object schema") {
        val schema = Schema[Rectangle]
        val json = schema.toJsonSchema

        // Should be an object schema
        assert(json("type").str == "object")
        assert(json.obj.contains("properties"))
        assert(json.obj.contains("required"))

        // Check properties
        val props = json("properties")
        assert(props.obj.contains("width"))
        assert(props.obj.contains("height"))
        assert(props("width")("type").str == "number")
        assert(props("height")("type").str == "number")

        // Check required fields
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("width", "height"))

        // Should NOT have type discriminator when used individually
        assert(!props.obj.contains("type"))
      }

      test("Triangle case class generates correct object schema") {
        val schema = Schema[Triangle]
        val json = schema.toJsonSchema

        // Should be an object schema
        assert(json("type").str == "object")

        // Check properties
        val props = json("properties")
        assert(props.obj.contains("base"))
        assert(props.obj.contains("height"))
        assert(props("base")("type").str == "number")
        assert(props("height")("type").str == "number")

        // Check required fields
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("base", "height"))
      }

      test("TextData case class with string field") {
        val schema = Schema[TextData]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        val props = json("properties")
        assert(props("content")("type").str == "string")

        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("content"))
      }

      test("NumberData case class with multiple fields") {
        val schema = Schema[NumberData]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        val props = json("properties")
        assert(props("value")("type").str == "number")
        assert(props("count")("type").str == "integer")

        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("value", "count"))
      }

      test("BooleanData case class with boolean field") {
        val schema = Schema[BooleanData]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        val props = json("properties")
        assert(props("flag")("type").str == "boolean")

        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("flag"))
      }

      test("WithOptional case class handles optional fields correctly") {
        val schema = Schema[WithOptional]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        val props = json("properties")
        assert(props("required")("type").str == "string")
        assert(props("optional")("type").str == "integer")

        // Only required field should be in required array
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("required"))
        assert(!required.contains("optional"))
      }

      test("AllRequired case class has all fields required") {
        val schema = Schema[AllRequired]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        val props = json("properties")
        assert(props("name")("type").str == "string")
        assert(props("age")("type").str == "integer")

        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name", "age"))
      }

      test("EmptyCase generates minimal object schema") {
        val schema = Schema[EmptyCase]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")

        // EmptyCase should not have properties or required fields
        // since it has no fields
        assert(!json.obj.contains("properties"))
        assert(!json.obj.contains("required"))

        // Should be a minimal object schema
        assert(json.obj.size == 1) // Only "type" field
      }

      test("NonEmptyCase has correct string field") {
        val schema = Schema[NonEmptyCase]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        val props = json("properties")
        assert(props("value")("type").str == "string")

        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("value"))
      }
    }

    test("Case class vs sealed trait schema differences") {
      test("Individual case class vs same case class in sealed trait") {
        // Individual Circle schema
        val circleSchema = Schema[Circle]
        val circleJson = circleSchema.toJsonSchema

        // Circle as part of Shape sealed trait
        val shapeSchema = Schema[Shape]
        val shapeJson = shapeSchema.toJsonSchema
        val circleInShape = shapeJson("oneOf").arr.find { variant =>
          variant("properties").obj.contains("radius")
        }.get

        // Individual Circle should NOT have type discriminator
        assert(!circleJson("properties").obj.contains("type"))
        val circleRequired = circleJson("required").arr.map(_.str).toSet
        assert(circleRequired == Set("radius"))

        // Circle in sealed trait SHOULD have type discriminator
        assert(circleInShape("properties").obj.contains("type"))
        val shapeCircleRequired = circleInShape("required").arr.map(_.str).toSet
        assert(shapeCircleRequired == Set("radius", "type"))

        // Both should have same radius property
        assert(circleJson("properties")("radius")("type").str == "number")
        assert(circleInShape("properties")("radius")("type").str == "number")
      }

      test("Case class compilation and instantiation works") {
        // Verify case classes can be instantiated and used
        val circle = Circle(5.0)
        val rectangle = Rectangle(10.0, 20.0)
        val triangle = Triangle(8.0, 12.0)

        // Verify schemas can be summoned
        val circleSchema = summon[Schema[Circle]]
        val rectangleSchema = summon[Schema[Rectangle]]
        val triangleSchema = summon[Schema[Triangle]]

        // Verify they generate valid JSON schemas
        assert(circleSchema.schema.toJsonSchema("type").str == "object")
        assert(rectangleSchema.schema.toJsonSchema("type").str == "object")
        assert(triangleSchema.schema.toJsonSchema("type").str == "object")
      }
    }
  }
}
