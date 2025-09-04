package caskchez.examples.zerotoapp

import chez.derivation.Schema
import upickle.default.*

/**
 * Zero-to-App: Models stub
 *
 * Follow docs/zero-to-app.md Step 1 to refine these models.
 * You can start with these defaults and iterate progressively.
 */
@Schema.title("CreateUser")
case class CreateUser(
    // TODO: Add/adjust annotations per docs (minLength, format, minimum)
    name: String,
    email: String,
    age: Int
) derives Schema, ReadWriter

@Schema.title("User")
case class User(
    id: String,
    name: String,
    email: String,
    age: Int
) derives Schema, ReadWriter

@Schema.title("ProfileSummary")
case class ProfileSummary(
    // Free-form profile text to infer interests from
    @Schema.minLength(5) text: String
) derives Schema, ReadWriter

@Schema.title("UserInterests")
case class UserInterests(
    @Schema.description("Primary, strong interests")
    @Schema.minItems(0)
    primary: List[String] = Nil,
    @Schema.description("Secondary interests")
    @Schema.minItems(0)
    secondary: List[String] = Nil,
    @Schema.description("Normalized tags for indexing/search")
    @Schema.minItems(0)
    tags: List[String] = Nil,
    @Schema.description("Optional notes or rationale")
    notes: Option[String] = None
) derives Schema, ReadWriter
