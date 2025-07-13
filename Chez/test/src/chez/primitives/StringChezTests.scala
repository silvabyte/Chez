package chez.primitives

import utest.*
import chez.primitives.*
import chez.*
import chez.validation.*

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
    
    test("enum validation - moved to EnumChez") {
      // Note: enum validation is now handled by EnumChez, not StringChez
      // This test exists for backward compatibility documentation
      val enumSchema = chez.primitives.EnumChez.fromStrings("red", "green", "blue")
      assert(enumSchema.validateString("red") == Nil)
      assert(enumSchema.validateString("green") == Nil)
      assert(enumSchema.validateString("blue") == Nil)
      assert(enumSchema.validateString("yellow").nonEmpty)
      assert(enumSchema.validateString("Red").nonEmpty) // case sensitive
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
    
    test("StringChez validates ujson.Str values correctly") {
      val schema = Chez.String(minLength = Some(3), maxLength = Some(10))
      
      test("valid string") {
        val result = schema.validate(ujson.Str("hello"))
        assert(result.isValid)
        assert(result.errors.isEmpty)
      }
      
      test("string too short") {
        val result = schema.validate(ujson.Str("hi"))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[ValidationError.MinLengthViolation])
      }
      
      test("string too long") {
        val result = schema.validate(ujson.Str("this is way too long"))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[ValidationError.MaxLengthViolation])
      }
    }
    
    test("StringChez rejects non-string ujson.Value types with TypeMismatch error") {
      val schema = Chez.String()
      
      test("number value") {
        val result = schema.validate(ujson.Num(42))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "number")
      }
      
      test("boolean value") {
        val result = schema.validate(ujson.Bool(true))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "boolean")
      }
      
      test("null value") {
        val result = schema.validate(ujson.Null)
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "null")
      }
      
      test("array value") {
        val result = schema.validate(ujson.Arr(ujson.Str("test")))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "array")
      }
      
      test("object value") {
        val result = schema.validate(ujson.Obj("key" -> ujson.Str("value")))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "object")
      }
    }
    
    test("All existing validation logic works with ujson.Value") {
      test("pattern validation") {
        val schema = Chez.String(pattern = Some("^[A-Z]+$"))
        
        val validResult = schema.validate(ujson.Str("HELLO"))
        assert(validResult.isValid)
        
        val invalidResult = schema.validate(ujson.Str("hello"))
        assert(!invalidResult.isValid)
        assert(invalidResult.errors.head.isInstanceOf[ValidationError.PatternMismatch])
      }
      
      test("const validation") {
        val schema = Chez.String(const = Some("expected"))
        
        val validResult = schema.validate(ujson.Str("expected"))
        assert(validResult.isValid)
        
        val invalidResult = schema.validate(ujson.Str("unexpected"))
        assert(!invalidResult.isValid)
        assert(invalidResult.errors.head.isInstanceOf[ValidationError.TypeMismatch])
      }
      
      test("format validation") {
        val emailSchema = Chez.String(format = Some("email"))
        
        val validResult = emailSchema.validate(ujson.Str("test@example.com"))
        assert(validResult.isValid)
        
        val invalidResult = emailSchema.validate(ujson.Str("not-an-email"))
        assert(!invalidResult.isValid)
        assert(invalidResult.errors.head.isInstanceOf[ValidationError.InvalidFormat])
      }
      
      test("multiple constraint validation") {
        val schema = Chez.String(
          minLength = Some(5),
          maxLength = Some(15), 
          pattern = Some("^[a-z]+$")
        )
        
        val validResult = schema.validate(ujson.Str("hello"))
        assert(validResult.isValid)
        
        val invalidResult = schema.validate(ujson.Str("Hi"))
        assert(!invalidResult.isValid)
        // Should have both minLength and pattern violations
        assert(invalidResult.errors.length == 2)
      }
    }
    
    test("Error paths are correctly set using ValidationContext") {
      val schema = Chez.String(minLength = Some(5))
      
      test("default context") {
        val result = schema.validate(ujson.Str("hi"))
        assert(!result.isValid)
        val error = result.errors.head.asInstanceOf[ValidationError.MinLengthViolation]
        assert(error.path == "/")
      }
      
      test("custom context path") {
        val context = ValidationContext("/user/name")
        val result = schema.validate(ujson.Str("hi"), context)
        assert(!result.isValid)
        val error = result.errors.head.asInstanceOf[ValidationError.MinLengthViolation]
        assert(error.path == "/user/name")
      }
      
      test("nested context path") {
        val context = ValidationContext()
          .withProperty("user")
          .withProperty("profile")
          .withProperty("name")
        val result = schema.validate(ujson.Str("hi"), context)
        assert(!result.isValid)
        val error = result.errors.head.asInstanceOf[ValidationError.MinLengthViolation]
        assert(error.path == "/user/profile/name")
      }
      
      test("type mismatch error path") {
        val context = ValidationContext("/data/field")
        val result = schema.validate(ujson.Num(42), context)
        assert(!result.isValid)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.path == "/data/field")
      }
    }
    
    test("ValidationResult is returned with proper valid/invalid state") {
      val schema = Chez.String(minLength = Some(3))
      
      test("valid state") {
        val result = schema.validate(ujson.Str("hello"))
        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult.Valid.type])
      }
      
      test("invalid state") {
        val result = schema.validate(ujson.Str("hi"))
        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult.Invalid])
      }
    }
    
    test("Existing validate(String) method continues to work unchanged") {
      val schema = Chez.String(minLength = Some(3), pattern = Some("^[a-z]+$"))
      
      test("valid string") {
        val errors = schema.validate("hello")
        assert(errors.isEmpty)
      }
      
      test("invalid string") {
        val errors = schema.validate("Hi")
        assert(errors.length == 2) // Both minLength and pattern violations
        assert(errors.exists(_.isInstanceOf[ValidationError.MinLengthViolation]))
        assert(errors.exists(_.isInstanceOf[ValidationError.PatternMismatch]))
      }
      
      test("string too short") {
        val errors = schema.validate("hi")
        assert(errors.length == 1)
        assert(errors.head.isInstanceOf[ValidationError.MinLengthViolation])
      }
      
      test("errors still use default path") {
        val errors = schema.validate("X")
        assert(errors.nonEmpty)
        // Original method should still use "/" path
        errors.foreach { error =>
          val path = error match {
            case ValidationError.MinLengthViolation(_, _, path) => path
            case ValidationError.PatternMismatch(_, _, path) => path
            case _ => "/"
          }
          assert(path == "/")
        }
      }
    }
    
    test("Direct schema validation") {
      val schema = Chez.String(const = Some("test"))
      
      test("direct validate works") {
        val result = schema.validate(ujson.Str("test"), ValidationContext())
        assert(result.isValid)
        
        val invalidResult = schema.validate(ujson.Str("wrong"), ValidationContext())
        assert(!invalidResult.isValid)
      }
      
      test("validate with custom path works") {
        val context = ValidationContext("/custom/path")
        val result = schema.validate(ujson.Str("wrong"), context)
        assert(!result.isValid)
        assert(result.errors.head.asInstanceOf[ValidationError.TypeMismatch].path == "/custom/path")
      }
    }
  }
}