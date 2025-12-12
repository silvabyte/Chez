package boogieloops.schema.derivation

import utest.*
import boogieloops.schema.*
import boogieloops.schema.complex.*
import boogieloops.schema.derivation.CollectionSchemas.given

object VectorDerivationSchemaTests extends TestSuite {

  val tests = Tests {
    test("Vector[T] derivation") {
      test("Vector[String] generates array without uniqueItems") {
        val schema = Schematic[Vector[String]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        // Vector should NOT have uniqueItems constraint (allows duplicates)
        assert(!json.obj.contains("uniqueItems"))
        assert(json.obj.contains("items"))
        assert(json("items")("type").str == "string")
      }

      test("Vector[Int] generates array without uniqueItems") {
        val schema = Schematic[Vector[Int]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))
        assert(json("items")("type").str == "integer")
      }

      test("Vector[Boolean] generates array without uniqueItems") {
        val schema = Schematic[Vector[Boolean]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))
        assert(json("items")("type").str == "boolean")
      }

      test("Vector[Double] generates array without uniqueItems") {
        val schema = Schematic[Vector[Double]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))
        assert(json("items")("type").str == "number")
      }

      test("Vector[Long] generates array without uniqueItems") {
        val schema = Schematic[Vector[Long]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))
        assert(json("items")("type").str == "integer")
      }

      test("Vector[Float] generates array without uniqueItems") {
        val schema = Schematic[Vector[Float]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))
        assert(json("items")("type").str == "number")
      }
    }

    test("Vector vs Set vs List comparison") {
      test("Vector, Set, and List have different uniqueItems behavior") {
        val vectorSchema = Schematic[Vector[String]]
        val setSchema = Schematic[Set[String]]
        val listSchema = Schematic[List[String]]

        val vectorJson = vectorSchema.toJsonSchema
        val setJson = setSchema.toJsonSchema
        val listJson = listSchema.toJsonSchema

        // Vector: no uniqueItems constraint (allows duplicates)
        assert(!vectorJson.obj.contains("uniqueItems"))

        // Set: uniqueItems=true (no duplicates)
        assert(setJson.obj.contains("uniqueItems"))
        assert(setJson("uniqueItems").bool == true)

        // List: no uniqueItems constraint (allows duplicates)
        assert(!listJson.obj.contains("uniqueItems"))

        // All should be arrays with same item type
        assert(vectorJson("type").str == "array")
        assert(setJson("type").str == "array")
        assert(listJson("type").str == "array")

        assert(vectorJson("items")("type").str == "string")
        assert(setJson("items")("type").str == "string")
        assert(listJson("items")("type").str == "string")
      }
    }

    test("Vector integration with case classes") {
      test("case class with Vector[T] fields") {
        case class DataCollection(
            name: String,
            measurements: Vector[Double],
            labels: Vector[String],
            flags: Vector[Boolean]
        ) derives Schematic

        val schema = Schematic[DataCollection]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("properties"))
        assert(json.obj.contains("required"))

        val props = json("properties")

        // Check name field
        assert(props("name")("type").str == "string")

        // Check measurements field (Vector[Double])
        assert(props("measurements")("type").str == "array")
        assert(!props("measurements").obj.contains("uniqueItems"))
        assert(props("measurements")("items")("type").str == "number")

        // Check labels field (Vector[String])
        assert(props("labels")("type").str == "array")
        assert(!props("labels").obj.contains("uniqueItems"))
        assert(props("labels")("items")("type").str == "string")

        // Check flags field (Vector[Boolean])
        assert(props("flags")("type").str == "array")
        assert(!props("flags").obj.contains("uniqueItems"))
        assert(props("flags")("items")("type").str == "boolean")

        // Check required fields
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name", "measurements", "labels", "flags"))
      }

      test("case class with nested Vectors") {
        case class NestedVectors(
            matrix: Vector[Vector[Int]],
            metadata: Map[String, Vector[String]]
        ) derives Schematic

        val schema = Schematic[NestedVectors]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check matrix field (Vector[Vector[Int]])
        assert(props("matrix")("type").str == "array")
        assert(!props("matrix").obj.contains("uniqueItems"))
        val matrixItems = props("matrix")("items")
        assert(matrixItems("type").str == "array")
        assert(!matrixItems.obj.contains("uniqueItems"))
        assert(matrixItems("items")("type").str == "integer")

        // Check metadata field (Map[String, Vector[String]])
        assert(props("metadata")("type").str == "object")
        assert(props("metadata").obj.contains("additionalProperties"))
        val metadataItems = props("metadata")("additionalProperties")
        assert(metadataItems("type").str == "array")
        assert(!metadataItems.obj.contains("uniqueItems"))
        assert(metadataItems("items")("type").str == "string")
      }

      test("case class with Optional Vector fields") {
        case class OptionalVectors(
            name: String,
            tags: Option[Vector[String]],
            scores: Vector[Int] = Vector.empty
        ) derives Schematic

        val schema = Schematic[OptionalVectors]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check optional tags field
        assert(props("tags")("type").str == "array")
        assert(!props("tags").obj.contains("uniqueItems"))
        assert(props("tags")("items")("type").str == "string")

        // Check default scores field
        assert(props("scores")("type").str == "array")
        assert(!props("scores").obj.contains("uniqueItems"))
        assert(props("scores")("items")("type").str == "integer")

        // Check required fields (tags should be optional, scores has default)
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name"))
      }

      test("case class with mixed collection types") {
        case class MixedCollections(
            items: Vector[String],
            uniqueItems: Set[String],
            dynamicItems: List[String]
        ) derives Schematic

        val schema = Schematic[MixedCollections]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Vector: no uniqueItems constraint
        assert(props("items")("type").str == "array")
        assert(!props("items").obj.contains("uniqueItems"))

        // Set: uniqueItems=true
        assert(props("uniqueItems")("type").str == "array")
        assert(props("uniqueItems").obj.contains("uniqueItems"))
        assert(props("uniqueItems")("uniqueItems").bool == true)

        // List: no uniqueItems constraint
        assert(props("dynamicItems")("type").str == "array")
        assert(!props("dynamicItems").obj.contains("uniqueItems"))
      }
    }

    test("Vector with complex value types") {
      test("Vector[Case Class] works correctly") {
        case class Person(name: String, age: Int) derives Schematic

        val schema = Schematic[Vector[Person]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))

        val items = json("items")
        assert(items("type").str == "object")
        assert(items.obj.contains("properties"))
        assert(items("properties")("name")("type").str == "string")
        assert(items("properties")("age")("type").str == "integer")
      }

      test("Vector[List[T]] works correctly") {
        val schema = Schematic[Vector[List[String]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))

        val items = json("items")
        assert(items("type").str == "array")
        assert(!items.obj.contains("uniqueItems"))
        assert(items("items")("type").str == "string")
      }

      test("Vector[Set[T]] works correctly") {
        val schema = Schematic[Vector[Set[String]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems")) // Vector allows duplicates

        val items = json("items")
        assert(items("type").str == "array")
        assert(items.obj.contains("uniqueItems")) // Set requires uniqueness
        assert(items("uniqueItems").bool == true)
        assert(items("items")("type").str == "string")
      }

      test("Vector[Map[String, T]] works correctly") {
        val schema = Schematic[Vector[Map[String, Int]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))

        val items = json("items")
        assert(items("type").str == "object")
        assert(items.obj.contains("additionalProperties"))
        assert(items("additionalProperties")("type").str == "integer")
      }
    }

    test("Vector derivation edge cases") {
      test("Vector[Option[T]] works correctly") {
        val schema = Schematic[Vector[Option[String]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))

        val items = json("items")
        assert(items("type").str == "string")
        // Note: Option[T] schema doesn't add nullable in JSON Schema 2020-12
        // It's handled at the validation level
      }

      test("Empty Vector types compile correctly") {
        // This test ensures the derivation compiles for various Vector types
        val stringVectorSchema = Schematic[Vector[String]]
        val intVectorSchema = Schematic[Vector[Int]]
        val boolVectorSchema = Schematic[Vector[Boolean]]
        val doubleVectorSchema = Schematic[Vector[Double]]
        val longVectorSchema = Schematic[Vector[Long]]
        val floatVectorSchema = Schematic[Vector[Float]]

        // All should generate array schemas without uniqueItems
        assert(!stringVectorSchema.toJsonSchema.obj.contains("uniqueItems"))
        assert(!intVectorSchema.toJsonSchema.obj.contains("uniqueItems"))
        assert(!boolVectorSchema.toJsonSchema.obj.contains("uniqueItems"))
        assert(!doubleVectorSchema.toJsonSchema.obj.contains("uniqueItems"))
        assert(!longVectorSchema.toJsonSchema.obj.contains("uniqueItems"))
        assert(!floatVectorSchema.toJsonSchema.obj.contains("uniqueItems"))
      }

      test("Vector maintains order semantics in schema") {
        // Vector preserves insertion order, unlike Set
        // This is reflected in the schema by NOT having uniqueItems constraint
        val schema = Schematic[Vector[String]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(!json.obj.contains("uniqueItems"))
        // This allows [\"a\", \"b\", \"a\"] which is valid for Vector but not Set
      }
    }
  }
}
