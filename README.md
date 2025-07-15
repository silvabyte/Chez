# Chez: JSON Schema for Scala 3

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/silvabyte/scalaschemaz)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Chez** is a comprehensive JSON Schema library for Scala 3 that provides TypeBox-like ergonomics with compile-time type safety and runtime JSON Schema compliance.

## Modules

- **[Chez](./docs/chez.md)**: Core JSON Schema generation, validation, and derivation library
- **[CaskChez](./docs/caskchez.md)**: Cask HTTP framework integration with automatic request/response validation

## Quick Start

```scala
import chez.derivation.Schema

// Annotation-based derivation (the main hotness!)
case class User(
  @Schema.description("User's full name")
  @Schema.minLength(1) name: String,
  
  @Schema.description("Email address")
  @Schema.format("email") email: String,
  
  @Schema.description("User's age")
  @Schema.minimum(0) @Schema.default(18) age: Int
) derives Schema

// Generate JSON Schema automatically
val userSchema = Schema[User]
val jsonSchema = userSchema.toJsonSchema

// Automatic validation
val validationResult = userSchema.validate(userData)

// HTTP endpoints with automatic validation
@CaskChez.post("/users", RouteSchema(body = Some(Schema[User])))
def createUser(validatedRequest: ValidatedRequest) = {
  // Request automatically validated against schema!
  validatedRequest.getBody[User] match {
    case Right(user) => processUser(user)
    case Left(error) => handleError(error)
  }
}
```

## Installation

```scala
// build.mill
def ivyDeps = Agg(
  ivy"com.lihaoyi::upickle:4.1.0",
  ivy"com.lihaoyi::cask:0.9.7", // For CaskChez
  // Add Chez modules when published
)
```

## Documentation

- **[Chez Core Library](./docs/chez.md)** - Schema creation, validation, composition, and annotation-based derivation
- **[CaskChez HTTP Integration](./docs/caskchez.md)** - HTTP framework integration, automatic validation, and OpenAPI generation

## Examples

```bash
# Run examples
./mill Chez.runMain chez.examples.BasicUsage
./mill CaskChez.runMain caskchez.examples.UserCrudAPI

# Run tests
make test                 # Run all tests
make test-integration    # Run basic CRUD tests
make test-comprehensive  # Run advanced scenario tests
make test-web-validation # Run HTTP validation tests
```

## Features

✅ **Full JSON Schema 2020-12 compliance**  
✅ **Scala 3 match types and union types**  
✅ **TypeBox-like ergonomics**  
✅ **Annotation-based schema derivation**  
✅ **Automatic HTTP request/response validation**  
✅ **OpenAPI specification generation**  
✅ **Zero boilerplate validation**  
✅ **Comprehensive validation for body, query, params, headers**

## License

MIT License. See [LICENSE](LICENSE) for details.
