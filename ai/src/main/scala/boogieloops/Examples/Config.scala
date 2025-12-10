package boogieloops.ai.examples

import dotenv.{DotEnv}

object Config {
  // scalafix:off DisableSyntax.var
  // Disabling because we need mutable state to lazy-load the .env configuration file
  // only when initialize() is called, supporting different env directories at runtime
  @volatile private var _dotenv: Option[DotEnv] = None
  // scalafix:on DisableSyntax.var

  // Initialize the configuration with a specified directory
  def initialize(envDirectory: String): Unit = {
    _dotenv = Some(DotEnv.load(s"$envDirectory/.env"))
  }

  private def dotenv: Option[DotEnv] = _dotenv

  // Accessors for configuration values - return Option instead of throwing
  def getOpenAIKey: Option[String] =
    dotenv.flatMap(_.get("OPENAI_API_KEY"))

  def getAnthropicKey: Option[String] =
    dotenv.flatMap(_.get("ANTHROPIC_API_KEY"))

  // For backward compatibility, provide methods that return default empty strings
  lazy val OPENAI_API_KEY: String = getOpenAIKey.getOrElse("")
  lazy val ANTHROPIC_API_KEY: String = getAnthropicKey.getOrElse("")

  // Generic get method for any config value
  def get(key: String, default: String = ""): String = {
    dotenv
      .flatMap(_.get(key))
      .getOrElse(default)
  }

  // Get integer config value
  def getInt(key: String, default: Int = 0): Int = {
    dotenv
      .flatMap(_.get(key))
      .map(_.toIntOption.getOrElse(default))
      .getOrElse(default)
  }
  def getLong(key: String, default: Long = 0) =
    dotenv.flatMap(_.get(key)).map(_.toLongOption.getOrElse(default)).getOrElse(default)
}
