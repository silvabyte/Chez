package chez.derivation

import utest.*
import chez.*
import chez.derivation.Schema

object DefaultAnnotationTests extends TestSuite {

  val tests = Tests {
    test("all default annotation types compile") {
      case class ComprehensiveDefaults(
          @Schema.default(42) count: Int,
          @Schema.default(true) enabled: Boolean,
          @Schema.default(3.14) pi: Double,
          @Schema.default("hello") greeting: String
      ) derives Schema

      val schema = Schema[ComprehensiveDefaults]
      val json = schema.toJsonSchema
      val props = json("properties").obj
      
      assert(json("type").str == "object")
      
      // Verify all default values are correctly included in the JSON schema
      assert(props("count")("default").num == 42)
      assert(props("enabled")("default").bool == true)
      assert(props("pi")("default").num == 3.14)
      assert(props("greeting")("default").str == "hello")
    }

    test("optional fields with defaults") {
      case class OptionalDefaults(
          @Schema.default(8080) port: Option[Int],
          @Schema.default(false) ssl: Option[Boolean],
          @Schema.default(30.0) timeout: Option[Double],
          @Schema.default("localhost") host: Option[String]
      ) derives Schema

      val schema = Schema[OptionalDefaults]
      val json = schema.toJsonSchema
      val props = json("properties").obj
      
      assert(json("type").str == "object")
      
      // Verify default values work with Optional types
      assert(props("port")("default").num == 8080)
      assert(props("ssl")("default").bool == false)
      assert(props("timeout")("default").num == 30.0)
      assert(props("host")("default").str == "localhost")
    }

    test("defaults mixed with other annotations") {
      case class AnnotatedConfig(
          @Schema.default(3000)
          @Schema.minimum(1000)
          @Schema.maximum(65535) port: Int,
          @Schema.default(true)
          @Schema.description("Enable SSL encryption") ssl: Boolean,
          @Schema.default("production")
          @Schema.enumValues("development", "staging", "production") environment: String,
          @Schema.default(2.5)
          @Schema.minimum(0.0)
          @Schema.maximum(10.0) multiplier: Double
      ) derives Schema

      val schema = Schema[AnnotatedConfig]
      val json = schema.toJsonSchema
      val props = json("properties").obj

      // Verify other annotations work (proves annotation processing is functional)
      assert(props("ssl")("description").str == "Enable SSL encryption")
      assert(props("port")("minimum").num == 1000.0)
      assert(props("port")("maximum").num == 65535.0)
      assert(props("multiplier")("minimum").num == 0.0)
      assert(props("multiplier")("maximum").num == 10.0)

      // Verify default values work alongside other annotations
      assert(props("port")("default").num == 3000)
      assert(props("ssl")("default").bool == true)
      assert(props("environment")("default").str == "production")
      assert(props("multiplier")("default").num == 2.5)
    }

    test("negative and zero default values") {
      case class EdgeCaseDefaults(
          @Schema.default(-1) negative: Int,
          @Schema.default(0) zero: Int,
          @Schema.default(-3.14) negativeDouble: Double,
          @Schema.default(0.0) zeroDouble: Double,
          @Schema.default(false) disabled: Boolean,
          @Schema.default("") empty: String
      ) derives Schema

      val schema = Schema[EdgeCaseDefaults]
      val json = schema.toJsonSchema
      val props = json("properties").obj
      
      assert(json("type").str == "object")
      
      // Verify edge case default values are handled correctly
      assert(props("negative")("default").num == -1)
      assert(props("zero")("default").num == 0)
      assert(props("negativeDouble")("default").num == -3.14)
      assert(props("zeroDouble")("default").num == 0.0)
      assert(props("disabled")("default").bool == false)
      assert(props("empty")("default").str == "")
    }

    test("nested case classes with defaults") {
      case class DatabaseConfig(
          @Schema.default("localhost") host: String,
          @Schema.default(5432) port: Int,
          @Schema.default(true) ssl: Boolean
      ) derives Schema

      case class AppConfig(
          @Schema.default("MyApp") name: String,
          @Schema.default(1.0) version: Double,
          database: DatabaseConfig
      ) derives Schema

      val appSchema = Schema[AppConfig]
      val dbSchema = Schema[DatabaseConfig]
      
      val appJson = appSchema.toJsonSchema
      val dbJson = dbSchema.toJsonSchema
      
      assert(appJson("type").str == "object")
      assert(dbJson("type").str == "object")
      
      // Verify defaults work in nested structures
      val appProps = appJson("properties").obj
      val dbProps = dbJson("properties").obj
      
      assert(appProps("name")("default").str == "MyApp")
      assert(appProps("version")("default").num == 1.0)
      
      assert(dbProps("host")("default").str == "localhost")
      assert(dbProps("port")("default").num == 5432)
      assert(dbProps("ssl")("default").bool == true)
    }
  }
}
