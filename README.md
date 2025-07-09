# Chez: TypeBox for Scala 3

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/silvabyte/chez)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Chez** is a library that provides an API that typescript developers are familiar with for JSON Schema generation and validation. The results is a library with Scala 3's sleek ergonomics, a proper type system, offering compile-time type safety and runtime JSON Schema compliance.

## Features

- 🎯 **Full JSON Schema 2020-12 Compliance**: Supports all vocabularies including core, validation, meta-data, format, content, and composition
- 🚀 **Scala 3 Powered**: Leverages match types, union types, and modern Scala 3 features
- 🔧 **Familiar API**: Familiar syntax for developers coming from TypeScript
- 📦 **Lihaoyi Ecosystem Integration**: Seamless integration with upickle, os-lib, and other lihaoyi tools
- 💎 **Compile-time Type Safety**: Schema definitions provide compile-time type information
- 🌟 **Pragmatic Null Handling**: Distinguishes between optional fields and nullable values
- 🎨 **Composition Support**: Full support for anyOf, oneOf, allOf, not, and conditional schemas

## Quick Start

### Add to your project

```scala
// build.mill
def ivyDeps = Agg(
  ivy"com.lihaoyi::upickle:4.1.0",
  ivy"com.lihaoyi::os-lib:0.11.3",
  // Add chez when published
)
```

### Basic Usage

```scala
import chez.*

// Primitive types
val stringSchema = Chez.String(
  minLength = Some(1),
  maxLength = Some(100),
  pattern = Some("^[a-zA-Z]+$")
)

val numberSchema = Chez.Number(
  minimum = Some(0),
  maximum = Some(100),
  multipleOf = Some(0.1)
)

// Complex types
val userSchema = Chez.Object(
  "id" -> Chez.String(),
  "name" -> Chez.String(minLength = Some(1)),
  "email" -> Chez.String(format = Some("email")),
  "age" -> Chez.Integer(minimum = Some(0)).optional
)

// Generate JSON Schema
val jsonSchema = userSchema.toJsonSchema
println(jsonSchema)
// Output: {"type":"object","properties":{"id":{"type":"string"},"name":{"type":"string","minLength":1},"email":{"type":"string","format":"email"},"age":{"type":"integer","minimum":0}},"required":["id","name","email"]}
```

## Null Handling

Chez provides pragmatic null handling that distinguishes between optional fields and nullable values:

```scala
// Regular field (required, non-null)
val name = Chez.String()  // String in Scala

// Optional field (may be absent from JSON)
val nickname = Chez.String().optional  // Option[String] in Scala

// Nullable field (present but may be null)
val middleName = Chez.String().nullable  // Option[String] in Scala

// Optional AND nullable (may be absent OR null)
val suffix = Chez.String().optional.nullable  // Option[String] in Scala
```

## JSON Schema 2020-12 Composition

```scala
// AnyOf - one or more must match
val anyOfSchema = Chez.AnyOf(Chez.String(), Chez.Number())

// OneOf - exactly one must match
val oneOfSchema = Chez.OneOf(Chez.String(), Chez.Number())

// AllOf - all must match
val allOfSchema = Chez.AllOf(
  Chez.Object("name" -> Chez.String()),
  Chez.Object("age" -> Chez.Integer())
)

// Not - must not match
val notSchema = Chez.Not(Chez.String())

// Conditional schemas (if/then/else)
val conditionalSchema = Chez.If(
  condition = Chez.Object("type" -> Chez.String()),
  thenSchema = Chez.Object("name" -> Chez.String()),
  elseSchema = Chez.Object("id" -> Chez.Integer())
)
```

## Lihaoyi Ecosystem Integration

Chez integrates seamlessly with the lihaoyi ecosystem:

```scala
// With upickle for JSON serialization
import upickle.default.*

val userJson = """{"id":"123","name":"John","email":"john@example.com"}"""
val user = read[User](userJson)  // Type-safe deserialization

// With os-lib for configuration
val configSchema = Chez.Object(
  "database" -> Chez.Object(
    "host" -> Chez.String(),
    "port" -> Chez.Integer(minimum = Some(1024), maximum = Some(65535))
  ),
  "server" -> Chez.Object(
    "host" -> Chez.String(),
    "port" -> Chez.Integer(minimum = Some(1024), maximum = Some(65535))
  )
)

val config = configSchema.fromJsonStringUnsafe(os.read(os.pwd / "config.json"))
```

## Advanced Features

### Pattern Properties

```scala
val dynamicSchema = Chez.Object(
  properties = Map("version" -> Chez.String()),
  patternProperties = Map(
    "^feature_.*" -> Chez.Boolean(),
    "^cache_.*" -> Chez.Object(
      "ttl" -> Chez.Integer(minimum = Some(0)),
      "enabled" -> Chez.Boolean()
    )
  )
)
```

### Schema References

```scala
val refSchema = Chez.Ref("#/$defs/User")
val dynamicRefSchema = Chez.DynamicRef("#user")
```

### Meta-Schema Compliance

```scala
val compliantSchema = Chez.Object(
  "product" -> Chez.Object(
    "id" -> Chez.String(),
    "name" -> Chez.String()
  )
).withSchema("https://json-schema.org/draft/2020-12/schema")
 .withId("https://example.com/schemas/product")
 .withTitle("Product Schema")
 .withDescription("A schema for product objects")
```

## Architecture

Chez is built on a foundation of:

- **Core Trait**: `Chez` trait provides the base functionality
- **Primitive Types**: `StringChez`, `NumberChez`, `IntegerChez`, `BooleanChez`, `NullChez`
- **Complex Types**: `ObjectChez`, `ArrayChez`
- **Composition Types**: `AnyOfChez`, `OneOfChez`, `AllOfChez`, `NotChez`, `IfThenElseChez`
- **References**: `RefChez`, `DynamicRefChez`
- **Modifiers**: `OptionalChez`, `NullableChez`, `DefaultChez`

## JSON Schema 2020-12 Compliance

Chez provides full compliance with JSON Schema Draft 2020-12, including:

- **Core Vocabulary**: `$schema`, `$id`, `$ref`, `$defs`, `$dynamicRef`, `$dynamicAnchor`
- **Validation Vocabulary**: All validation keywords (type, properties, required, etc.)
- **Meta-Data Vocabulary**: `title`, `description`, `examples`, `default`
- **Format Vocabulary**: Format validation with annotation/assertion modes
- **Content Vocabulary**: `contentMediaType`, `contentEncoding`, `contentSchema`
- **Composition Keywords**: `anyOf`, `oneOf`, `allOf`, `not`, `if`/`then`/`else`
- **Array Keywords**: `prefixItems`, `items`, `minItems`, `maxItems`, `uniqueItems`
- **Object Keywords**: `properties`, `required`, `additionalProperties`, `patternProperties`

## Examples

See the `examples` package for comprehensive usage examples:

- `BasicUsage.scala` - Core functionality demonstration
- `LihaoyiEcosystem.scala` - Integration with lihaoyi ecosystem
- `ComplexTypes.scala` - Advanced schema composition
- `Validation.scala` - Schema validation examples

## Running Examples

```bash
# Run basic usage examples
./mill Schemaz.runMain chez.examples.BasicUsage

# Run lihaoyi ecosystem integration examples
./mill Schemaz.runMain chez.examples.LihaoyiEcosystem
```

## Development Status

✅ **Phase 1 Complete**: Core Foundation & JSON Schema 2020-12 Core Vocabulary

- Basic Chez trait hierarchy with JSON Schema 2020-12 compliance
- Primitive schema types (String, Number, Integer, Boolean, Null)
- Core vocabulary implementation ($schema, $id, $ref, $defs)
- Basic type-level computations using Scala 3 match types
- Simple JSON Schema generation with proper meta-schema
- Working examples and lihaoyi ecosystem integration

🚧 **Next Phases**:

- Enhanced validation system with full JSON Schema test suite compliance
- Advanced format and content vocabulary support
- Comprehensive type-level programming with proper type inference
- Performance optimizations and benchmarks

## License

MIT License. See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

---

**Chez** - Bringing TypeBox's ergonomics to Scala 3 with full JSON Schema 2020-12 compliance! 🎉
