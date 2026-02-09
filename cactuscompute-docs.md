Kotlin Multiplatform SDK
Complete guide to using Cactus SDK in Kotlin applications

Cactus Kotlin
Official Kotlin Multiplatform library for Cactus, a framework for deploying LLM and STT models locally in your app.

Installation
Dependency List

Add to settings.gradle.kts


dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
Gradle Build

Add to your KMP project's build.gradle.kts:


kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.cactuscompute:cactus:1.2.0-beta")
            }
        }
    }
}
Grant Permissions

Add the permissions to your manifest (Android):


<uses-permission android:name="android.permission.INTERNET" /> // for model downloads
<uses-permission android:name="android.permission.RECORD_AUDIO" /> // for transcription
Context Initialization (Required)
Before using any Cactus SDK functionality, you must initialize the context in your Activity's onCreate() method:


import com.cactus.CactusContextInitializer
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Cactus context (required)
        CactusContextInitializer.initialize(this)
        // ... rest of your code
    }
}
Language Model (LLM)
The CactusLM class provides text completion capabilities with support for function calling (WIP).

Basic Usage

import com.cactus.CactusLM
import com.cactus.CactusInitParams
import com.cactus.CactusCompletionParams
import com.cactus.ChatMessage
import kotlinx.coroutines.runBlocking
runBlocking {
    val lm = CactusLM()
    try {
        // Download a model by slug (e.g., "qwen3-0.6", "gemma3-270m")
        // If no model is specified, it defaults to "qwen3-0.6"
        // Throws exception on failure
        lm.downloadModel("qwen3-0.6")
        
        // Initialize the model
        // Throws exception on failure
        lm.initializeModel(
            CactusInitParams(
                model = "qwen3-0.6",
                contextSize = 2048
            )
        )
        // Generate completion with default parameters
        val result = lm.generateCompletion(
            messages = listOf(
                ChatMessage(content = "Hello, how are you?", role = "user")
            )
        )
        result?.let { response ->
            if (response.success) {
                println("Response: ${response.response}")
                println("Tokens per second: ${response.tokensPerSecond}")
                println("Time to first token: ${response.timeToFirstTokenMs}ms")
            }
        }
    } finally {
        // Clean up
        lm.unload()
    }
}
Inference Modes
CactusLM supports hybrid inference modes that allow fallback between local and cloud-based processing:


val result = lm.generateCompletion(
    messages = listOf(ChatMessage(content = "Hello!", role = "user")),
    params = CactusCompletionParams(
        mode = InferenceMode.LOCAL_FIRST, // Try local first, fallback to cloud
        cactusToken = "your_cactus_token"
    )
)
Available modes:

InferenceMode.LOCAL - Local inference only (default)
InferenceMode.REMOTE - Cloud-based inference only
InferenceMode.LOCAL_FIRST - Try local first, fallback to cloud
InferenceMode.REMOTE_FIRST - Try cloud first, fallback to local
To get a cactusToken, join our Discord community and contact us.

Streaming Completions

    val result = lm.generateCompletion(
    messages = listOf(ChatMessage("Tell me a story", "user")),
    params = CactusCompletionParams(maxTokens = 200),
        onToken = { token, tokenId ->
        print(token) // Print each token as it's generated
    }
)
Available Models
You can get a list of available models:


lm.getModels()
Function Calling

import com.cactus.models.ToolParameter
import com.cactus.models.createTool
    val tools = listOf(
        createTool(
            name = "get_weather",
            description = "Get current weather for a location",
            parameters = mapOf(
                "location" to ToolParameter(
                    type = "string",
                    description = "City name",
                    required = true
                )
            )
        )
    )
    val result = lm.generateCompletion(
    messages = listOf(ChatMessage("What's the weather in New York?", "user")),
        params = CactusCompletionParams(
        maxTokens = 100,
            tools = tools
        )
    )
Vision (Multimodal)

runBlocking {
    val lm = CactusLM()
    // Download and initialize a vision model
    val visionModel = lm.getModels().first { it.supports_vision }
    lm.downloadModel(visionModel.slug)
    lm.initializeModel(CactusInitParams(model = visionModel.slug))
    var streamingResponse = ""
    val result = lm.generateCompletion(
        messages = listOf(
            ChatMessage("You are a helpful AI assistant that can analyze images.", "system"),
            ChatMessage(
                content = "What do you see in this image?",
                role = "user",
                images = listOf("/path/to/image.jpg")
            )
        ),
        params = CactusCompletionParams(maxTokens = 300),
        onToken = { token, _ ->
            streamingResponse += token
            print(token)
        }
    )
    println("\n\nFinal analysis: ${result?.response}")
    lm.unload()
}
LLM API Reference
CactusLM Class
suspend fun downloadModel(model: String = "qwen3-0.6"): Boolean - Download a model
suspend fun initializeModel(params: CactusInitParams): Boolean - Initialize model for inference
suspend fun generateCompletion(messages: List<ChatMessage>, params: CactusCompletionParams = CactusCompletionParams(), onToken: CactusStreamingCallback? = null): CactusCompletionResult? - Generate text completion with support for streaming and inference modes
suspend fun generateEmbedding(text: String, modelName: String? = null): CactusEmbeddingResult? - Generate text embeddings
fun unload() - Free model from memory
suspend fun getModels(): List<CactusModel> - Get available LLM models
fun isLoaded(): Boolean - Check if model is loaded
Data Classes
CactusInitParams(model: String?, contextSize: Int?) - Model initialization parameters
CactusCompletionParams(temperature: Double, topK: Int, topP: Double, maxTokens: Int, stopSequences: List<String>, tools: List<Tool>?, mode: InferenceMode, cactusToken: String?, model: String?) - Completion parameters
ChatMessage(content: String, role: String, timestamp: Long?, images: List<String>) - Chat message format
CactusCompletionResult - Contains response, timing metrics, and success status
CactusEmbeddingResult(success: Boolean, embeddings: List<Double>, dimension: Int, errorMessage: String?) - Embedding generation result
InferenceMode - Enum for inference modes (LOCAL, REMOTE, LOCAL_FIRST, REMOTE_FIRST)
Tool Filtering
The ToolFilterService enables intelligent filtering of tools to optimize function calling by selecting only the most relevant tools for a given user query.

Configuration
Configure tool filtering when creating a CactusLM instance:


import com.cactus.CactusLM
import com.cactus.services.ToolFilterConfig
import com.cactus.services.ToolFilterStrategy
// Enable with default settings (SIMPLE strategy, max 3 tools)
val lm = CactusLM(
    enableToolFiltering = true,
    toolFilterConfig = ToolFilterConfig.simple(maxTools = 3)
)
// Custom configuration with SIMPLE strategy
val lm = CactusLM(
    enableToolFiltering = true,
    toolFilterConfig = ToolFilterConfig(
        strategy = ToolFilterStrategy.SIMPLE,
        maxTools = 5,
        similarityThreshold = 0.3
    )
)
// Use SEMANTIC strategy for more accurate filtering
val lm = CactusLM(
    enableToolFiltering = true,
    toolFilterConfig = ToolFilterConfig(
        strategy = ToolFilterStrategy.SEMANTIC,
        maxTools = 3,
        similarityThreshold = 0.5
    )
)
// Disable tool filtering
val lm = CactusLM(enableToolFiltering = false)
Configuration Parameters
strategy - The filtering algorithm: SIMPLE (default, fast) or SEMANTIC (slower but more accurate)
maxTools - Maximum number of tools to pass to the model (default: null, meaning no limit)
similarityThreshold - Minimum score required for a tool to be included (default: 0.3)
Embeddings
The CactusLM class also provides text embedding generation capabilities for semantic similarity, search, and other NLP tasks.

Basic Usage

import com.cactus.CactusLM
import com.cactus.CactusInitParams
import kotlinx.coroutines.runBlocking
runBlocking {
    val lm = CactusLM()
    
    // Download and initialize a model (same as for completions)
    lm.downloadModel("qwen3-0.6")
    lm.initializeModel(CactusInitParams(model = "qwen3-0.6", contextSize = 2048))
    // Generate embeddings for a text
    val result = lm.generateEmbedding(
        text = "This is a sample text for embedding generation"
    )
    result?.let { embedding ->
        if (embedding.success) {
            println("Embedding dimension: ${embedding.dimension}")
            println("Embedding vector length: ${embedding.embeddings.size}")
        } else {
            println("Embedding generation failed: ${embedding.errorMessage}")
        }
    }
    lm.unload()
}
Embedding API Reference
CactusLM Class (Embedding Methods)
suspend fun generateEmbedding(text: String, modelName: String? = null): CactusEmbeddingResult? - Generate embeddings for the given text.
suspend fun getModels(): List<CactusModel> - Get a list of available models. Results are cached locally to reduce network requests.
fun unload() - Unload the current model and free resources.
fun isLoaded(): Boolean - Check if a model is currently loaded.
Data Classes
CactusInitParams(model: String? = null, contextSize: Int? = null) - Parameters for model initialization.
CactusCompletionParams(model: String? = null, temperature: Double? = null, topK: Int? = null, topP: Double? = null, maxTokens: Int = 200, stopSequences: List<String> = listOf("<|im_end|>", "<end_of_turn>"), tools: List<CactusTool> = emptyList(), mode: InferenceMode = InferenceMode.LOCAL, cactusToken: String? = null) - Parameters for text completion.
CactusCompletionResult(success: Boolean, response: String? = null, timeToFirstTokenMs: Double? = null, totalTimeMs: Double? = null, tokensPerSecond: Double? = null, prefillTokens: Int? = null, decodeTokens: Int? = null, totalTokens: Int? = null, toolCalls: List<ToolCall>? = emptyList()) - The result of a text completion.
CactusEmbeddingResult(success: Boolean, embeddings: List<Double> = listOf(), dimension: Int? = null, errorMessage: String? = null) - The result of embedding generation.
ChatMessage(content: String, role: String, timestamp: Long? = null) - A chat message with role (e.g., "user", "assistant").
CactusModel(created_at: String, slug: String, download_url: String, size_mb: Int, supports_tool_calling: Boolean, supports_vision: Boolean, name: String, isDownloaded: Boolean = false, quantization: Int = 8) - Information about an available model.
InferenceMode - Enum for selecting inference mode (LOCAL, REMOTE, LOCAL_FIRST, REMOTE_FIRST).
ToolCall(name: String, arguments: Map<String, String>) - Represents a tool call returned by the model.
CactusTool(type: String = "function", function: CactusFunction) - Defines a tool that can be called by the model.
CactusFunction(name: String, description: String, parameters: ToolParametersSchema) - Function definition for a tool.
ToolParametersSchema(type: String = "object", properties: Map<String, ToolParameter>, required: List<String>) - Schema for tool parameters.
ToolParameter(type: String, description: String, required: Boolean = false) - A parameter definition for a tool.
Helper Functions
createTool(name: String, description: String, parameters: Map<String, ToolParameter>): CactusTool - Helper function to create a tool with the correct schema.
Speech-to-Text (STT)
The CactusSTT class provides speech recognition capabilities using on-device models from Whisper.

Choosing a Transcription Provider
You can select a transcription provider when initializing CactusSTT. The available providers are:

TranscriptionProvider.WHISPER: Uses Whisper for transcription.

import com.cactus.CactusSTT
import com.cactus.TranscriptionProvider
// Initialize with the Whisper provider (default)
val stt = CactusSTT() 
Basic Usage

import com.cactus.CactusSTT
import kotlinx.coroutines.runBlocking
runBlocking {
    val stt = CactusSTT()
    // Download STT model (default: whisper-tiny)
    val downloadSuccess = stt.download("whisper-tiny)
    
    // Initialize the model
    val initSuccess = stt.init("whisper-tiny")
    // Transcribe from file
    val result = stt.transcribe(
        filePath = "/path/to/audio.wav",
        params = CactusTranscriptionParams()
    )
    result?.let { transcription ->
        if (transcription.success) {
            println("Transcribed: ${transcription.text}")
            println("Processing time: ${transcription.processingTime}ms")
        }
    }
    // Stop transcription
    stt.stop()
}
Available Voice Models

// Get list of available voice models
stt.getVoiceModels()
// Check if model is downloaded
stt.isModelDownloaded("whisper-tiny")
STT API Reference
CactusSTT Class
CactusSTT() - Constructor for the STT service.
suspend fun downloadModel(model: String = "whisper-tiny") - Download an STT model (e.g., "whisper-tiny" or "whisper-base"). Defaults to last initialized model. Throws exception on failure.
suspend fun initializeModel(params: CactusInitParams) - Initialize an STT model for transcription using the model specified in params. Throws exception on failure.
suspend fun transcribe(filePath: String, prompt: String = "<|startoftranscript|><|en|><|transcribe|><|notimestamps|>", params: CactusTranscriptionParams = CactusTranscriptionParams(), onToken: CactusStreamingCallback? = null, mode: TranscriptionMode = TranscriptionMode.LOCAL, apiKey: String? = null): CactusTranscriptionResult? - Transcribe speech from an audio file. Supports streaming via the onToken callback and different transcription modes (local, remote, and fallbacks).
suspend fun warmUpWispr(apiKey: String) - Warms up the remote Wispr service for lower latency.
fun isReady(): Boolean - Check if the STT service is initialized and ready.
suspend fun getVoiceModels(): List<VoiceModel> - Get a list of available voice models. Results are cached locally to reduce network requests.
suspend fun isModelDownloaded(modelName: String = "whisper-tiny"): Boolean - Check if a specific model has been downloaded. Defaults to last initialized model.
Data Classes
CactusInitParams(model: String? = null, contextSize: Int? = null) - Parameters for model initialization (shared with CactusLM).
CactusTranscriptionParams(model: String? = null, maxTokens: Int = 512, stopSequences: List<String> = listOf("<|im_end|>", "<end_of_turn>")) - Parameters for controlling speech transcription.
CactusTranscriptionResult(success: Boolean, text: String? = null, totalTimeMs: Double? = null) - The result of a transcription.
VoiceModel(created_at: String, slug: String, download_url: String, size_mb: Int, quantization: Int, isDownloaded: Boolean = false) - Contains information about an available voice model.
TranscriptionMode - Enum for transcription mode (LOCAL, REMOTE, LOCAL_FIRST, REMOTE_FIRST).
Platform-Specific Setup
Android
Works automatically - native libraries included
Requires API 24+ (Android 7.0)
ARM64 architecture supported
iOS
Add the Cactus package dependency in Xcode
Requires iOS 12.0+
Supports ARM64 and Simulator ARM64
Building the Library
To build the library from source:


# Build the library and publish to localMaven
./build_library.sh
Telemetry Setup (Optional)
Cactus comes with powerful built-in telemetry that lets you monitor your projects. Create a token on the Cactus dashboard and get started with a one-line setup in your app:


import com.cactus.services.CactusTelemetry
// Initialize telemetry for usage analytics (optional)
CactusTelemetry.setTelemetryToken("your_token_here")
Example App
Navigate to the example app and run it:


cd kotlin/example
# For desktop
./gradlew :composeApp:run
# For Android/iOS - use Android Studio or Xcode
The example app demonstrates:

Model downloading and initialization
Text completion with streaming
Function calling
Speech-to-text transcription
Error handling and status management
Performance Tips
Model Selection: Choose smaller models for faster inference on mobile devices
Context Size: Reduce context size for lower memory usage
Memory Management: Always call unload() when done with models
Batch Processing: Reuse initialized models for multiple completions
Model Caching: Use getModels() for efficient model discovery - results are cached locally to reduce network requests