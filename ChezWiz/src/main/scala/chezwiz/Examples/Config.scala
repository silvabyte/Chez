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
}
