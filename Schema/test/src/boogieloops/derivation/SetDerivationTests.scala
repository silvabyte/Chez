package boogieloops.schema.derivation

import utest.*
import boogieloops.schema.*
import boogieloops.schema.complex.*
import boogieloops.schema.derivation.CollectionSchemas.given

object SetDerivationSchemaTests extends TestSuite {

  val tests = Tests {
    test("Set[T] derivation") {
      test("Set[String] generates array with uniqueItems=true") {
        val schema = Schematic[Set[String]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json.obj.contains("uniqueItems"))
        assert(json("uniqueItems").bool == true)
        assert(json.obj.contains("items"))
        assert(json("items")("type").str == "string")
      }

      test("Set[Int] generates array with uniqueItems=true") {
        val schema = Schematic[Set[Int]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("uniqueItems").bool == true)
        assert(json("items")("type").str == "integer")
      }

      test("Set[Boolean] generates array with uniqueItems=true") {
        val schema = Schematic[Set[Boolean]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("uniqueItems").bool == true)
        assert(json("items")("type").str == "boolean")
      }

      test("Set[Double] generates array with uniqueItems=true") {
        val schema = Schematic[Set[Double]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("uniqueItems").bool == true)
        assert(json("items")("type").str == "number")
      }
    }

    test("Set integration with case classes") {
      test("case class with Set[T] fields") {
        case class UserPreferences(
            name: String,
            tags: Set[String],
            favoriteNumbers: Set[Int]
        ) derives Schematic

        val schema = Schematic[UserPreferences]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("properties"))
        assert(json.obj.contains("required"))

        val props = json("properties")

        // Check name field
        assert(props("name")("type").str == "string")

        // Check tags field (Set[String])
        assert(props("tags")("type").str == "array")
        assert(props("tags")("uniqueItems").bool == true)
        assert(props("tags")("items")("type").str == "string")

        // Check favoriteNumbers field (Set[Int])
        assert(props("favoriteNumbers")("type").str == "array")
        assert(props("favoriteNumbers")("uniqueItems").bool == true)
        assert(props("favoriteNumbers")("items")("type").str == "integer")

        // Check required fields
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name", "tags", "favoriteNumbers"))
      }

      test("case class with nested Sets") {
        case class NestedSets(
            groups: Set[Set[String]],
            metadata: Map[String, Set[Int]]
        ) derives Schematic

        val schema = Schematic[NestedSets]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check groups field (Set[Set[String]])
        assert(props("groups")("type").str == "array")
        assert(props("groups")("uniqueItems").bool == true)
        val groupsItems = props("groups")("items")
        assert(groupsItems("type").str == "array")
        assert(groupsItems("uniqueItems").bool == true)
        assert(groupsItems("items")("type").str == "string")

        // Check metadata field (Map[String, Set[Int]])
        assert(props("metadata")("type").str == "object")
        assert(props("metadata").obj.contains("additionalProperties"))
        val metadataItems = props("metadata")("additionalProperties")
        assert(metadataItems("type").str == "array")
        assert(metadataItems("uniqueItems").bool == true)
        assert(metadataItems("items")("type").str == "integer")
      }

      test("case class with Optional Set fields") {
        case class OptionalSets(
            name: String,
            tags: Option[Set[String]],
            categories: Set[String] = Set.empty
        ) derives Schematic

        val schema = Schematic[OptionalSets]
        val json = schema.toJsonSchema

        val props = json("properties")

        // Check optional tags field
        assert(props("tags")("type").str == "array")
        assert(props("tags")("uniqueItems").bool == true)
        assert(props("tags")("items")("type").str == "string")

        // Check default categories field
        assert(props("categories")("type").str == "array")
        assert(props("categories")("uniqueItems").bool == true)
        assert(props("categories")("items")("type").str == "string")

        // Check required fields (tags should be optional, categories has default)
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name"))
      }
    }

    test("Set with complex value types") {
      test("Set[Case Class] works correctly") {
        case class Person(name: String, age: Int) derives Schematic

        val schema = Schematic[Set[Person]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("uniqueItems").bool == true)

        val items = json("items")
        assert(items("type").str == "object")
        assert(items.obj.contains("properties"))
        assert(items("properties")("name")("type").str == "string")
        assert(items("properties")("age")("type").str == "integer")
      }

      test("Set[List[T]] works correctly") {
        val schema = Schematic[Set[List[String]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("uniqueItems").bool == true)

        val items = json("items")
        assert(items("type").str == "array")
        assert(items("items")("type").str == "string")
      }

      test("Set[Map[String, T]] works correctly") {
        val schema = Schematic[Set[Map[String, Int]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("uniqueItems").bool == true)

        val items = json("items")
        assert(items("type").str == "object")
        assert(items.obj.contains("additionalProperties"))
        assert(items("additionalProperties")("type").str == "integer")
      }
    }

    test("Set derivation edge cases") {
      test("Set[Option[T]] works correctly") {
        val schema = Schematic[Set[Option[String]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("uniqueItems").bool == true)

        val items = json("items")
        assert(items("type").str == "string")
        // Note: Option[T] schema doesn't add nullable in JSON Schema 2020-12
        // It's handled at the validation level
      }

      test("Empty Set types compile correctly") {
        // This test ensures the derivation compiles for various Set types
        val stringSetSchema = Schematic[Set[String]]
        val intSetSchema = Schematic[Set[Int]]
        val boolSetSchema = Schematic[Set[Boolean]]
        val doubleSetSchema = Schematic[Set[Double]]
        val longSetSchema = Schematic[Set[Long]]
        val floatSetSchema = Schematic[Set[Float]]

        // All should generate array schemas with uniqueItems=true
        assert(stringSetSchema.toJsonSchema("uniqueItems").bool == true)
        assert(intSetSchema.toJsonSchema("uniqueItems").bool == true)
        assert(boolSetSchema.toJsonSchema("uniqueItems").bool == true)
        assert(doubleSetSchema.toJsonSchema("uniqueItems").bool == true)
        assert(longSetSchema.toJsonSchema("uniqueItems").bool == true)
        assert(floatSetSchema.toJsonSchema("uniqueItems").bool == true)
      }
    }
  }
}
