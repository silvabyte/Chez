# BoogieLoops Kit: General Purpose Utilities

Lightweight utilities for Scala 3 applications. Currently includes `DotEnv` for unified environment variable access from `.env` files and system environment.

## Install

Mill:

```scala
mvn"dev.boogieloop::kit:0.5.6"
```

SBT:

```scala
libraryDependencies += "dev.boogieloop" %% "kit" % "0.5.6"
```

## DotEnv

Load environment variables from `.env` files with automatic fallback to system environment. Fully immutable and thread-safe.

### Quickstart

```scala
import boogieloops.kit.DotEnv

// Load from .env file in current directory
val env = DotEnv.load()

// Access variables (checks .env first, then sys.env)
val dbHost = env.getOrElse("DATABASE_HOST", "localhost")
val apiKey = env.get("API_KEY")  // Option[String]

// Get all variables (unified view: sys.env + .env, file values win)
val allVars = env.all
```

### .env File Format

```bash
# Comments start with #
DATABASE_HOST=localhost
DATABASE_PORT=5432

# Quoted values (quotes are stripped)
API_KEY="sk-1234567890"
SECRET='single-quoted-value'

# Values with equals signs work
CONNECTION_STRING=host=localhost;port=5432;db=myapp
```

### API Reference

```scala
class DotEnv {
  // Get a value, checking .env first, then sys.env
  def get(key: String): Option[String]
  
  // Get with default fallback
  def getOrElse(key: String, default: String): String
  
  // Create new DotEnv with updated value (immutable)
  def withSet(key: String, value: String): DotEnv
  
  // Get all variables (sys.env merged with .env, file values take precedence)
  def all: Map[String, String]
}

object DotEnv {
  // Load from file (defaults to ".env" in current directory)
  def load(filePath: String = ".env", overrideExisting: Boolean = true): DotEnv
}
```

### Immutability

`DotEnv` is fully immutable. The `withSet` method returns a new instance:

```scala
val env1 = DotEnv.load()
val env2 = env1.withSet("NEW_KEY", "value")

env1.get("NEW_KEY")  // None (original unchanged)
env2.get("NEW_KEY")  // Some("value")
```

### Missing Files

If the `.env` file doesn't exist, `DotEnv.load()` returns an instance that falls back entirely to system environment:

```scala
val env = DotEnv.load("non_existent.env")
env.get("PATH")  // Still works - falls back to sys.env
env.all          // Contains all sys.env variables
```

### Override Behavior

The `overrideExisting` parameter controls how duplicate keys in the file are handled:

```scala
// .env contains:
// KEY=first
// KEY=second

DotEnv.load(overrideExisting = true).get("KEY")   // Some("second") - last wins
DotEnv.load(overrideExisting = false).get("KEY")  // Some("first") - first wins
```

## Testing

```bash
./mill Kit.test
# or
make test MODULE=Kit
```
