package chezwiz.agent

import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import chezwiz.agent.providers.{AnthropicProvider, OpenAIProvider, LLMProvider}

object AgentFactory:

  def createOpenAIAgent(
      name: String,
      instructions: String,
      apiKey: String,
      model: String = "gpt-4o-mini",
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None
  ): Try[Agent] =
    Try {
      val provider = new OpenAIProvider(apiKey)
      if !provider.validateModel(model) then
        throw new IllegalArgumentException(s"Model '$model' not supported by OpenAI provider")

      Agent(name, instructions, provider, model, temperature, maxTokens)
    }

  def createAnthropicAgent(
      name: String,
      instructions: String,
      apiKey: String,
      model: String = "claude-3-5-haiku-20241022",
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None
  ): Try[Agent] =
    Try {
      val provider = new AnthropicProvider(apiKey)
      if !provider.validateModel(model) then
        throw new IllegalArgumentException(s"Model '$model' not supported by Anthropic provider")

      Agent(name, instructions, provider, model, temperature, maxTokens)
    }

  def createAgent(
      name: String,
      instructions: String,
      provider: LLMProvider,
      model: String,
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None
  ): Try[Agent] =
    Try {
      if !provider.validateModel(model) then
        throw new IllegalArgumentException(
          s"Model '$model' not supported by ${provider.name} provider"
        )

      Agent(name, instructions, provider, model, temperature, maxTokens)
    }
