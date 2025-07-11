package chez.primitives

import utest.*
import chez.primitives.*

object StringChezTests extends TestSuite {

  val tests = Tests {
    test("basic string schema") {
      val schema = StringChez()
      assert(schema.validate("hello") == Nil)
      assert(schema.validate("") == Nil)
      assert(schema.validate("multiple words") == Nil)
    }
    
    test("min length validation") {
      val schema = StringChez(minLength = Some(5))
      assert(schema.validate("hello") == Nil)
      assert(schema.validate("longer string") == Nil)
      assert(schema.validate("hi").nonEmpty)
      assert(schema.validate("").nonEmpty)
    }
    
    test("max length validation") {
      val schema = StringChez(maxLength = Some(10))
      assert(schema.validate("short") == Nil)
      assert(schema.validate("exactly10!") == Nil)
      assert(schema.validate("this is way too long").nonEmpty)
    }
    
    test("pattern validation") {
      val schema = StringChez(pattern = Some("^[a-zA-Z]+$"))
      assert(schema.validate("hello") == Nil)
      assert(schema.validate("OnlyLetters") == Nil)
      assert(schema.validate("hello123").nonEmpty)
      assert(schema.validate("hello world").nonEmpty)
      assert(schema.validate("123").nonEmpty)
    }
    
    test("const validation") {
      val schema = StringChez(const = Some("exactly this"))
      assert(schema.validate("exactly this") == Nil)
      assert(schema.validate("not this").nonEmpty)
      assert(schema.validate("EXACTLY THIS").nonEmpty)
    }
    
    test("enum validation") {
      val schema = StringChez(enumValues = Some(List("red", "green", "blue")))
      assert(schema.validate("red") == Nil)
      assert(schema.validate("green") == Nil)
      assert(schema.validate("blue") == Nil)
      assert(schema.validate("yellow").nonEmpty)
      assert(schema.validate("Red").nonEmpty) // case sensitive
    }
    
    test("email format validation") {
      val schema = StringChez(format = Some("email"))
      assert(schema.validate("user@example.com") == Nil)
      assert(schema.validate("test.email+tag@domain.co.uk") == Nil)
      assert(schema.validate("not-an-email").nonEmpty)
      assert(schema.validate("@domain.com").nonEmpty)
      assert(schema.validate("user@").nonEmpty)
    }
    
    test("uri format validation") {
      val schema = StringChez(format = Some("uri"))
      assert(schema.validate("https://example.com") == Nil)
      assert(schema.validate("http://localhost:8080/path") == Nil)
      assert(schema.validate("ftp://files.example.org") == Nil)
      assert(schema.validate("not a uri").nonEmpty)
      assert(schema.validate("://invalid").nonEmpty)
    }
    
    test("uuid format validation") {
      val schema = StringChez(format = Some("uuid"))
      assert(schema.validate("550e8400-e29b-41d4-a716-446655440000") == Nil)
      assert(schema.validate("6ba7b810-9dad-11d1-80b4-00c04fd430c8") == Nil)
      assert(schema.validate("not-a-uuid").nonEmpty)
      assert(schema.validate("550e8400-e29b-41d4-a716").nonEmpty) // too short
      assert(schema.validate("550e8400-e29b-41d4-a716-446655440000-extra").nonEmpty)
    }
    
    test("date format validation") {
      val schema = StringChez(format = Some("date"))
      assert(schema.validate("2023-12-25") == Nil)
      assert(schema.validate("1999-01-01") == Nil)
      assert(schema.validate("not-a-date").nonEmpty)
      assert(schema.validate("12-25-2023").nonEmpty) // wrong format
      assert(schema.validate("2023/12/25").nonEmpty) // wrong separator
    }
    
    test("time format validation") {
      val schema = StringChez(format = Some("time"))
      assert(schema.validate("14:30:00") == Nil)
      assert(schema.validate("09:05:15") == Nil)
      assert(schema.validate("not-a-time").nonEmpty)
      assert(schema.validate("2:30:00").nonEmpty) // missing leading zero
      assert(schema.validate("14:30").nonEmpty) // missing seconds
    }
    
    test("date-time format validation") {
      val schema = StringChez(format = Some("date-time"))
      assert(schema.validate("2023-12-25T14:30:00") == Nil)
      assert(schema.validate("1999-01-01T00:00:00") == Nil)
      assert(schema.validate("not-datetime").nonEmpty)
      assert(schema.validate("2023-12-25 14:30:00").nonEmpty) // space instead of T
    }
    
    test("unknown format validation") {
      val schema = StringChez(format = Some("unknown-format"))
      // Unknown formats should not cause validation errors
      assert(schema.validate("any string") == Nil)
      assert(schema.validate("12345") == Nil)
    }
    
    test("combined validations") {
      val schema = StringChez(
        minLength = Some(8),
        maxLength = Some(20),
        pattern = Some("^[a-zA-Z0-9]+$")
      )
      assert(schema.validate("Password123") == Nil)
      assert(schema.validate("abcd1234") == Nil)
      assert(schema.validate("abc123").nonEmpty) // too short
      assert(schema.validate("this_password_is_way_too_long").nonEmpty) // too long
      assert(schema.validate("Password 123").nonEmpty) // contains space
    }
    
    test("json schema generation") {
      val schema = StringChez(
        minLength = Some(1),
        maxLength = Some(100),
        pattern = Some("^[a-zA-Z]+$")
      )
      val json = schema.toJsonSchema
      assert(json("type").str == "string")
      assert(json("minLength").num == 1)
      assert(json("maxLength").num == 100)
      assert(json("pattern").str == "^[a-zA-Z]+$")
    }
    
    test("default value support") {
      val schema = StringChez().withDefault(ujson.Str("hello"))
      val json = schema.toJsonSchema
      assert(json("default").str == "hello")
      
      val schemaEmpty = StringChez(minLength = Some(0)).withDefault(ujson.Str(""))
      val jsonEmpty = schemaEmpty.toJsonSchema
      assert(jsonEmpty("default").str == "")
      assert(jsonEmpty("minLength").num == 0)
      
      val schemaEmail = StringChez(format = Some("email")).withDefault(ujson.Str("user@example.com"))
      val jsonEmail = schemaEmail.toJsonSchema
      assert(jsonEmail("default").str == "user@example.com")
      assert(jsonEmail("format").str == "email")
    }
  }
}