package boogieloops.schema.primitives

import utest.*
import boogieloops.schema.primitives.*
import boogieloops.schema.validation.ValidationContext
import boogieloops.schema.*

object NullSchemaTests extends TestSuite {

  val tests = Tests {
    test("basic null schema") {
      val schema = NullSchema()
      val result = schema.validate(ujson.Null, ValidationContext())
      assert(result.isValid)
    }

    test("json schema generation") {
      val schema = NullSchema()
      val json = schema.toJsonSchema
      assert(json("type").str == "null")
    }

    test("schema with metadata") {
      val schema = NullSchema()
        .withTitle("Null Value")
        .withDescription("A null value schema")

      val json = schema.toJsonSchema
      assert(json("type").str == "null")
      assert(json("title").str == "Null Value")
      assert(json("description").str == "A null value schema")
    }

    test("default value support") {
      val schema = NullSchema().withDefault(ujson.Null)
      val json = schema.toJsonSchema
      assert(json("default") == ujson.Null)

      val schemaWithMetadata = NullSchema()
        .withTitle("Optional Field")
        .withDefault(ujson.Null)

      val jsonWithMetadata = schemaWithMetadata.toJsonSchema
      assert(jsonWithMetadata("default") == ujson.Null)
      assert(jsonWithMetadata("title").str == "Optional Field")
    }
  }
}
