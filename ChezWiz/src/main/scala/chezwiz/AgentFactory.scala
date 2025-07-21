package chezwiz.agent

import chezwiz.agent.providers.{OpenAIProvider, AnthropicProvider}
import scala.util.{Try, Success, Failure}

object AgentFactory:

  def createOpenAIAgent(
      name: String,
      instructions: String,
      apiKey: String,
      model: String,
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None
  ): Try[Agent] = {
    Try {
      val provider = new OpenAIProvider(apiKey)
      if !provider.validateModel(model) then
        throw LLMError(s"Model '$model' not supported by OpenAI provider")

      Agent(name, instructions, provider, model, temperature, maxTokens)
    }
  }

  def createAnthropicAgent(
      name: String,
      instructions: String,
      apiKey: String,
      model: String,
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None
  ): Try[Agent] = {
    Try {
      val provider = new AnthropicProvider(apiKey)
      if !provider.validateModel(model) then
        throw LLMError(s"Model '$model' not supported by Anthropic provider")

      Agent(name, instructions, provider, model, temperature, maxTokens)
    }
  }
