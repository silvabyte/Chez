package boogieloops.schema.primitives

import utest.*
import boogieloops.schema.primitives.*
import boogieloops.schema.validation.ValidationContext
import boogieloops.schema.*

object BooleanSchemaTests extends TestSuite {

  val tests = Tests {
    test("basic boolean schema") {
      val schema = BooleanSchema()
      val result1 = schema.validate(ujson.Bool(true), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Bool(false), ValidationContext())
      assert(result2.isValid)
    }

    test("const true validation") {
      val schema = BooleanSchema(const = Some(true))
      val result1 = schema.validate(ujson.Bool(true), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Bool(false), ValidationContext())
      assert(!result2.isValid)
    }

    test("const false validation") {
      val schema = BooleanSchema(const = Some(false))
      val result1 = schema.validate(ujson.Bool(false), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Bool(true), ValidationContext())
      assert(!result2.isValid)
    }

    test("json schema generation") {
      val schema = BooleanSchema()
      val json = schema.toJsonSchema
      assert(json("type").str == "boolean")
    }

    test("default value support") {
      val schema = BooleanSchema().withDefault(ujson.Bool(true))
      val json = schema.toJsonSchema
      assert(json("default").bool == true)

      val schemaFalse = BooleanSchema().withDefault(ujson.Bool(false))
      val jsonFalse = schemaFalse.toJsonSchema
      assert(jsonFalse("default").bool == false)
    }
  }
}
