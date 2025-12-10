package boogieloops.schema.derivation

import utest.*
import boogieloops.schema.*
import boogieloops.schema.complex.*
import boogieloops.schema.derivation.CollectionSchemas.given

object MapDerivationSchemaTests extends TestSuite {

  val tests = Tests {
    test("Map[String, V] derivation") {
      test("Map[String, Int] generates additionalProperties") {
        val schema = Schema[Map[String, Int]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("additionalProperties"))
        assert(!json.obj.contains("patternProperties"))

        val addProps = json("additionalProperties")
        assert(addProps("type").str == "integer")
      }

      test("Map[String, String] generates additionalProperties") {
        val schema = Schema[Map[String, String]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("additionalProperties"))

        val addProps = json("additionalProperties")
        assert(addProps("type").str == "string")
      }

      test("Map[String, Boolean] generates additionalProperties") {
        val schema = Schema[Map[String, Boolean]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("additionalProperties"))

        val addProps = json("additionalProperties")
        assert(addProps("type").str == "boolean")
      }
    }

    test("Map[K, V] non-string key derivation") {
      test("Map[Int, String] generates patternProperties") {
        val schema = Schema[Map[Int, String]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("patternProperties"))
        assert(!json.obj.contains("additionalProperties"))
        assert(json("description").str == "Map with integer keys")

        val patternProps = json("patternProperties")
        assert(patternProps.obj.contains(".*"))
        assert(patternProps(".*")("type").str == "string")
      }

      test("Map[Long, Double] generates patternProperties") {
        val schema = Schema[Map[Long, Double]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("patternProperties"))
        assert(json("description").str == "Map with long keys")

        val patternProps = json("patternProperties")
        assert(patternProps(".*")("type").str == "number")
      }

      test("Map[Boolean, Int] generates patternProperties") {
        val schema = Schema[Map[Boolean, Int]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("patternProperties"))
        assert(json("description").str == "Map with boolean keys")

        val patternProps = json("patternProperties")
        assert(patternProps(".*")("type").str == "integer")
      }
    }

    test("nested Map derivation") {
      test("Map[String, Map[String, Int]] works correctly") {
        val schema = Schema[Map[String, Map[String, Int]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("additionalProperties"))

        val outerAddProps = json("additionalProperties")
        assert(outerAddProps("type").str == "object")
        assert(outerAddProps.obj.contains("additionalProperties"))

        val innerAddProps = outerAddProps("additionalProperties")
        assert(innerAddProps("type").str == "integer")
      }

      test("Map[Int, Map[String, String]] works correctly") {
        val schema = Schema[Map[Int, Map[String, String]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("patternProperties"))
        assert(json("description").str == "Map with integer keys")

        val outerPatternProps = json("patternProperties")(".*")
        assert(outerPatternProps("type").str == "object")
        assert(outerPatternProps.obj.contains("additionalProperties"))

        val innerAddProps = outerPatternProps("additionalProperties")
        assert(innerAddProps("type").str == "string")
      }

      test("Map[String, Map[Int, Double]] works correctly") {
        val schema = Schema[Map[String, Map[Int, Double]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("additionalProperties"))

        val outerAddProps = json("additionalProperties")
        assert(outerAddProps("type").str == "object")
        assert(outerAddProps.obj.contains("patternProperties"))
        assert(outerAddProps("description").str == "Map with integer keys")

        val innerPatternProps = outerAddProps("patternProperties")(".*")
        assert(innerPatternProps("type").str == "number")
      }
    }

    test("Map integration with case classes") {
      test("case class with Map[String, V] fields") {
        case class UserProfile(
            name: String,
            metadata: Map[String, String],
            settings: Map[String, Boolean]
        ) derives Schema

        val schema = Schema[UserProfile]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("properties"))
        assert(json.obj.contains("required"))

        val props = json("properties")

        // Check name field
        assert(props("name")("type").str == "string")

        // Check metadata field (Map[String, String])
        assert(props("metadata")("type").str == "object")
        assert(props("metadata").obj.contains("additionalProperties"))
        assert(props("metadata")("additionalProperties")("type").str == "string")

        // Check settings field (Map[String, Boolean])
        assert(props("settings")("type").str == "object")
        assert(props("settings").obj.contains("additionalProperties"))
        assert(props("settings")("additionalProperties")("type").str == "boolean")

        // Check required fields
        val required = json("required").arr.map(_.str).toSet
        assert(required == Set("name", "metadata", "settings"))
      }

      test("case class with Map[K, V] non-string key fields") {
        case class GameData(
            playerName: String,
            scores: Map[Int, Double],
            achievements: Map[Long, String]
        ) derives Schema

        val schema = Schema[GameData]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        val props = json("properties")

        // Check scores field (Map[Int, Double])
        assert(props("scores")("type").str == "object")
        assert(props("scores").obj.contains("patternProperties"))
        assert(props("scores")("description").str == "Map with integer keys")
        assert(props("scores")("patternProperties")(".*")("type").str == "number")

        // Check achievements field (Map[Long, String])
        assert(props("achievements")("type").str == "object")
        assert(props("achievements").obj.contains("patternProperties"))
        assert(props("achievements")("description").str == "Map with long keys")
        assert(props("achievements")("patternProperties")(".*")("type").str == "string")
      }

      test("case class with mixed Map types") {
        case class MixedMaps(
            stringMap: Map[String, Int],
            intMap: Map[Int, String],
            nestedMap: Map[String, Map[Int, Boolean]]
        ) derives Schema

        val schema = Schema[MixedMaps]
        val json = schema.toJsonSchema

        val props = json("properties")

        // String key map should use additionalProperties
        assert(props("stringMap").obj.contains("additionalProperties"))
        assert(!props("stringMap").obj.contains("patternProperties"))

        // Int key map should use patternProperties
        assert(props("intMap").obj.contains("patternProperties"))
        assert(!props("intMap").obj.contains("additionalProperties"))

        // Nested map: outer string key, inner int key
        assert(props("nestedMap").obj.contains("additionalProperties"))
        val nestedInner = props("nestedMap")("additionalProperties")
        assert(nestedInner.obj.contains("patternProperties"))
        assert(nestedInner("description").str == "Map with integer keys")
      }
    }

    test("Map with complex value types") {
      test("Map[String, Case Class] works correctly") {
        case class Person(name: String, age: Int) derives Schema

        val schema = Schema[Map[String, Person]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("additionalProperties"))

        val addProps = json("additionalProperties")
        assert(addProps("type").str == "object")
        assert(addProps.obj.contains("properties"))
        assert(addProps("properties")("name")("type").str == "string")
        assert(addProps("properties")("age")("type").str == "integer")
      }

      test("Map[Int, List[String]] works correctly") {
        val schema = Schema[Map[Int, List[String]]]
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json.obj.contains("patternProperties"))

        val patternProps = json("patternProperties")(".*")
        assert(patternProps("type").str == "array")
        assert(patternProps("items")("type").str == "string")
      }
    }
  }
}
