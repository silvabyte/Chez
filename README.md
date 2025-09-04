# Chez: Type-Safe JSON Schema Ecosystem for Scala 3

<div align="center">
  <img src="assets/lechez.png" alt="Le Chez - The smoking cheese that judges your schemas" width="400">
  
  *Le Chez says: "Your schemas are probably wrong"*
</div>

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/silvabyte/scalaschemaz)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

An ecosystem of libraries for JSON Schema generation, HTTP validation & Automatic OpenAPI Spec Generation, and LLM integration in Scala 3. Each module can be used independently or together for a complete solution.

## Installation

Choose the modules you need for your project:

### Mill

```scala
// Just schema validation
mvn"com.silvabyte::chez:0.2.0"

// Add HTTP validation for Cask web framework
mvn"com.silvabyte::chez:0.2.0"
mvn"com.silvabyte::caskchez:0.2.0"

// Full ecosystem with LLM agent support
mvn"com.silvabyte::chez:0.2.0"
mvn"com.silvabyte::caskchez:0.2.0"
mvn"com.silvabyte::chezwiz:0.2.0"
```

### SBT

```scala
// Just schema validation
libraryDependencies += "com.silvabyte" %% "chez" % "0.2.0"

// Add HTTP validation for Cask web framework
libraryDependencies ++= Seq(
  "com.silvabyte" %% "chez" % "0.2.0",
  "com.silvabyte" %% "caskchez" % "0.2.0"
)

// Full ecosystem with LLM agent support
libraryDependencies ++= Seq(
  "com.silvabyte" %% "chez" % "0.2.0",
  "com.silvabyte" %% "caskchez" % "0.2.0",
  "com.silvabyte" %% "chezwiz" % "0.2.0"
)
```

## The Ecosystem

- Chez (core): Type‑safe JSON Schema derivation and validation
- CaskChez (web): Schema‑first HTTP validation + OpenAPI for Cask
- ChezWiz (AI): Type‑safe LLM agents with structured output

## Zero to App

Build a tiny, validated API in minutes. See [docs/zero-to-app.md](./docs/zero-to-app.md).
For local commands, run `make help`.

## Documentation

- Getting Started: [docs/README.md](./docs/README.md)
- Zero to App: [docs/zero-to-app.md](./docs/zero-to-app.md)
- Chez (core): [docs/chez.md](./docs/chez.md)
- CaskChez (web): [docs/caskchez.md](./docs/caskchez.md)
- ChezWiz (AI): [docs/chezwiz.md](./docs/chezwiz.md)
- Concepts: [docs/concepts.md](./docs/concepts.md)
- Troubleshooting: [docs/troubleshooting.md](./docs/troubleshooting.md)
- TypeScript SDK: [docs/typescript-sdk.md](./docs/typescript-sdk.md)

## Running Examples & Commands

- See all commands: `make help`
- Run examples: `make chez`, `make caskchez`, `make wiz`
- ChezWiz examples require `OPENAI_API_KEY` (or a local LLM endpoint)
- Test everything: `make test` (override `MODULE=Chez|CaskChez|ChezWiz`)

## License

MIT License. See [LICENSE](LICENSE) for details.
