package boogieloops.web.examples.zerotoapp

import boogieloops.schema.derivation.Schematic
import upickle.default.*

/**
 * Zero-to-App: Models stub
 *
 * Follow docs/zero-to-app.md Step 1 to refine these models.
 * You can start with these defaults and iterate progressively.
 */
@Schematic.title("CreateUser")
case class CreateUser(
    // TODO: Add/adjust annotations per docs (minLength, format, minimum)
    name: String,
    email: String,
    age: Int
) derives Schematic, ReadWriter

@Schematic.title("User")
case class User(
    id: String,
    name: String,
    email: String,
    age: Int
) derives Schematic, ReadWriter

@Schematic.title("ProfileSummary")
case class ProfileSummary(
    // Free-form profile text to infer interests from
    @Schematic.minLength(5) text: String
) derives Schematic, ReadWriter

@Schematic.title("UserInterests")
case class UserInterests(
    @Schematic.description("Primary, strong interests")
    @Schematic.minItems(0)
    primary: List[String] = Nil,
    @Schematic.description("Secondary interests")
    @Schematic.minItems(0)
    secondary: List[String] = Nil,
    @Schematic.description("Normalized tags for indexing/search")
    @Schematic.minItems(0)
    tags: List[String] = Nil,
    @Schematic.description("Optional notes or rationale")
    notes: Option[String] = None
) derives Schematic, ReadWriter
