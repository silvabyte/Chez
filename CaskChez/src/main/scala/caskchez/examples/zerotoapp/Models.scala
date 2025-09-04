package caskchez.examples.zerotoapp

import chez.derivation.Schema
import upickle.default.*

/**
 * Zero-to-App: Models stub
 *
 * Follow docs/zero-to-app.md Step 1 to refine these models.
 * You can start with these defaults and iterate progressively.
 */
case class CreateUser(
    // TODO: Add/adjust annotations per docs (minLength, format, minimum)
    name: String,
    email: String,
    age: Int
) derives Schema, ReadWriter

case class User(
    id: String,
    name: String,
    email: String,
    age: Int
) derives Schema, ReadWriter
