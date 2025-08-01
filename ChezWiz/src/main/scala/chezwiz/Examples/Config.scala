package chezwiz.agent.examples

import dotenv.{DotEnv}

object Config {
  private var dotenv: Option[DotEnv] = None

  // Initialize the configuration with a specified directory
  def initialize(envDirectory: String): Unit = {
    dotenv = Some(DotEnv.load(s"$envDirectory/.env"))
  }

  // Accessors for configuration values
  lazy val OPENAI_API_KEY: String = dotenv
    .getOrElse(throw new IllegalStateException("Config not initialized"))
    .get("OPENAI_API_KEY")
    .getOrElse(throw new IllegalStateException("OPENAI_API_KEY not set"))

  lazy val ANTHROPIC_API_KEY: String = dotenv
    .getOrElse(throw new IllegalStateException("Config not initialized"))
    .get("ANTHROPIC_API_KEY")
    .getOrElse(throw new IllegalStateException("ANTHROPIC_API_KEY not set"))

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
