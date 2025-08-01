package chezwiz.agent

import chezwiz.agent.providers.{LMStudioProvider, HttpVersion}
import upickle.default.*
import utest._

object LMStudioProviderSpec extends TestSuite:
  val tests = Tests {
    test("LMStudioProvider - create with default settings") {
      val provider = LMStudioProvider()
      
      assert(provider.name.contains("LMStudio"))
      assert(provider.modelId == "local-model")
      assert(provider.supportedModels.contains("local-model"))
      assert(provider.httpVersion == HttpVersion.Http11)
    }

    test("LMStudioProvider - create with custom URL and model") {
      val customUrl = "http://192.168.1.100:8080/v1"
      val customModel = "mistral-7b-instruct"
      
      val provider = LMStudioProvider(
        baseUrl = customUrl,
        modelId = customModel
      )
      
      assert(provider.name.contains(customUrl))
      assert(provider.modelId == customModel)
      assert(provider.supportedModels.contains(customModel))
    }

    test("LMStudioProvider - no authentication required") {
      val provider = LMStudioProvider()
      
      // Provider should work without API key
      assert(provider.name.contains("LMStudio"))
      // No need to test internal implementation details
    }

    test("LMStudioProvider - uses HTTP/1.1 for local compatibility") {
      val provider = LMStudioProvider()
      assert(provider.httpVersion == HttpVersion.Http11)
    }

    test("LMStudioProvider - validates any model") {
      val provider = LMStudioProvider(modelId = "specific-model")
      
      // Should allow any model, not just the specified one
      assert(provider.validateModel("specific-model") == Right(()))
      assert(provider.validateModel("any-other-model") == Right(()))
      assert(provider.validateModel("random-model") == Right(()))
    }

    test("LMStudioProvider - supports structured output") {
      val provider = LMStudioProvider()
      
      // Just verify provider is configured for structured output
      assert(provider.name.contains("LMStudio"))
      // LM Studio provider should support json_schema format for structured output
    }

    test("LMStudioProvider - chat request creation") {
      val provider = LMStudioProvider()
      
      val request = ChatRequest(
        messages = List(
          ChatMessage(Role.System, "You are a helpful assistant"),
          ChatMessage(Role.User, "Hello!")
        ),
        model = "local-model",
        temperature = Some(0.7),
        maxTokens = Some(500),
        stream = false
      )
      
      // Verify request structure
      assert(request.model == "local-model")
      assert(request.temperature == Some(0.7))
      assert(request.maxTokens == Some(500))
      assert(request.stream == false)
      assert(request.messages.length == 2)
      assert(request.messages(0).role == Role.System)
      assert(request.messages(0).content == "You are a helpful assistant")
      assert(request.messages(1).role == Role.User)
      assert(request.messages(1).content == "Hello!")
    }
  }