package chez.primitives

import utest.*
import chez.primitives.*
import chez.validation.ValidationContext
import chez.*

object NullChezTests extends TestSuite {

  val tests = Tests {
    test("basic null schema") {
      val schema = NullChez()
      val result = schema.validate(ujson.Null, ValidationContext())
      assert(result.isValid)
    }

    test("json schema generation") {
      val schema = NullChez()
      val json = schema.toJsonSchema
      assert(json("type").str == "null")
    }

    test("schema with metadata") {
      val schema = NullChez()
        .withTitle("Null Value")
        .withDescription("A null value schema")

      val json = schema.toJsonSchema
      assert(json("type").str == "null")
      assert(json("title").str == "Null Value")
      assert(json("description").str == "A null value schema")
    }

    test("default value support") {
      val schema = NullChez().withDefault(ujson.Null)
      val json = schema.toJsonSchema
      assert(json("default") == ujson.Null)

      val schemaWithMetadata = NullChez()
        .withTitle("Optional Field")
        .withDefault(ujson.Null)

      val jsonWithMetadata = schemaWithMetadata.toJsonSchema
      assert(jsonWithMetadata("default") == ujson.Null)
      assert(jsonWithMetadata("title").str == "Optional Field")
    }
  }
}
