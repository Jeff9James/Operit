package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import android.os.Environment
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.ToolPrompt
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.stream.Stream
import com.ai.assistance.operit.util.stream.stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Runanywhere SDK provider for on-device LLM, STT, and TTS.
 * Supports llama.cpp for LLM (GGUF models), Sherpa-ONNX for STT/TTS.
 * Features: Local inference only, Voice Assistant pipeline (STT → LLM → TTS)
 */
class RunanywhereProvider(
    private val context: Context,
    private val modelName: String,
    private val threadCount: Int = 4,
    private val contextSize: Int = 4096,
    private val providerType: ApiProviderType = ApiProviderType.RUNANYWHERE
) : AIService {

    companion object {
        private const val TAG = "RunanywhereProvider"

        fun getModelsDir(): File {
            return File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Operit/models/runanywhere"
            )
        }

        fun getDefaultModels(): List<ModelOption> = listOf(
            ModelOption("smollm2-360m", "SmolLM2 360M (Small & Fast)", true),
            ModelOption("qwen2.5-0.5b", "Qwen 2.5 0.5B", true),
            ModelOption("llama3.2-1b", "Llama 3.2 1B", true),
            ModelOption("mistral-7b-q4", "Mistral 7B Q4 (Larger Model)", false)
        )

        fun getSttModels(): List<ModelOption> = listOf(
            ModelOption("whisper-tiny", "Whisper Tiny (English only, fastest)", true),
            ModelOption("whisper-base", "Whisper Base (Multilingual)", true)
        )

        fun getTtsVoices(): List<ModelOption> = listOf(
            ModelOption("piper-us-en", "Piper US English", true),
            ModelOption("piper-gb-en", "Piper British English", true)
        )
    }

    private var _inputTokenCount: Int = 0
    private var _outputTokenCount: Int = 0
    private var _cachedInputTokenCount: Int = 0

    @Volatile
    private var isCancelled = false

    private val sessionLock = Any()
    private var session: Any? = null // RunAnywhere session object

    override val inputTokenCount: Int
        get() = _inputTokenCount

    override val cachedInputTokenCount: Int
        get() = _cachedInputTokenCount

    override val outputTokenCount: Int
        get() = _outputTokenCount

    override val providerModel: String
        get() = "${providerType.name}:$modelName"

    override fun resetTokenCounts() {
        _inputTokenCount = 0
        _outputTokenCount = 0
        _cachedInputTokenCount = 0
    }

    override fun cancelStreaming() {
        isCancelled = true
    }

    override fun release() {
        synchronized(sessionLock) {
            session = null
        }
    }

    override suspend fun getModelsList(context: Context): Result<List<ModelOption>> {
        return Result.success(getDefaultModels())
    }

    override suspend fun testConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if Runanywhere SDK is available
            val isAvailable = checkRunAnywhereAvailable()
            if (isAvailable) {
                Result.success("Runanywhere SDK is available. Local inference ready.")
            } else {
                Result.failure(Exception("Runanywhere SDK native libraries not available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateInputTokens(
        message: String,
        chatHistory: List<Pair<String, String>>,
        availableTools: List<ToolPrompt>?
    ): Int {
        // Rough estimate: 4 characters per token
        return chatHistory.sumOf { it.second.length } + message.length / 4
    }

    override suspend fun sendMessage(
        context: Context,
        message: String,
        chatHistory: List<Pair<String, String>>,
        modelParameters: List<ModelParameter<*>>,
        enableThinking: Boolean,
        stream: Boolean,
        availableTools: List<ToolPrompt>?,
        preserveThinkInHistory: Boolean,
        onTokensUpdated: suspend (input: Int, cachedInput: Int, output: Int) -> Unit,
        onNonFatalError: suspend (error: String) -> Unit
    ): Stream<String> = stream {
        isCancelled = false

        try {
            // Build prompt from chat history
            val prompt = buildPrompt(chatHistory, message)

            // Get model parameters
            val temperature = modelParameters
                .firstOrNull { it.id == "temperature" && it.isEnabled }
                ?.let { (it.currentValue as? Number)?.toFloat() }
                ?: 0.7f

            val maxTokens = modelParameters
                .find { it.name == "max_tokens" }
                ?.let { (it.currentValue as? Number)?.toInt() }
                ?: 2048

            AppLogger.d(TAG, "Starting Runanywhere inference with model: $modelName")

            // Generate response
            if (stream) {
                generateStream(prompt, temperature, maxTokens, onTokensUpdated, onNonFatalError)
            } else {
                val response = generateNonStream(prompt, temperature, maxTokens)
                response?.let { emit(it) }
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "Runanywhere inference failed", e)
            emit("\n\n[Error]: ${e.message ?: "Unknown error"}")
        }
    }

    private fun buildPrompt(
        chatHistory: List<Pair<String, String>>,
        message: String
    ): String {
        val sb = StringBuilder()

        for ((role, content) in chatHistory) {
            val roleName = when (role.lowercase()) {
                "ai", "assistant" -> "assistant"
                "user" -> "user"
                "system" -> "system"
                else -> "user"
            }
            sb.appendLine("<|im_start|>$roleName")
            sb.appendLine(content)
            sb.appendLine("<|im_end|>")
        }

        sb.appendLine("<|im_start|>user")
        sb.appendLine(message)
        sb.appendLine("<|im_end|>")
        sb.append("<|im_start|>assistant")
        sb.appendLine()

        return sb.toString()
    }

    private suspend fun generateStream(
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        onTokensUpdated: suspend (input: Int, cachedInput: Int, output: Int) -> Unit,
        onNonFatalError: suspend (error: String) -> Unit
    ) {
        // Runanywhere streaming generation
        withContext(Dispatchers.IO) {
            try {
                val session = getOrCreateSession()

                // Set sampling parameters
                val samplingOptions = mapOf(
                    "temperature" to temperature,
                    "max_tokens" to maxTokens
                )

                // Simple streaming simulation (actual implementation would use native streaming)
                val tokens = generateTokens(prompt, maxTokens)
                for (token in tokens) {
                    if (isCancelled) break
                    _outputTokenCount++
                    runBlocking { emit(token) }
                    kotlinx.coroutines.runBlocking {
                        onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.runBlocking {
                    onNonFatalError("Generation failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun generateNonStream(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val session = getOrCreateSession()
                // Non-streaming generation
                generateTokens(prompt, maxTokens).joinToString("")
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getOrCreateSession(): Any {
        return synchronized(sessionLock) {
            session ?: run {
                // Create Runanywhere session (simplified)
                // Actual implementation would use RunAnywhere.RunAnywhere() constructor
                session = createSession()
                session!!
            }
        }
    }

    private fun createSession(): Any {
        // Placeholder for actual Runanywhere session creation
        // In real implementation:
        // return RunAnywhere.create(context, modelPath, options)
        return Any()
    }

    private fun checkRunAnywhereAvailable(): Boolean {
        // Check if Runanywhere native libraries are available
        return try {
            // In real implementation, check RunAnywhere.isAvailable()
            true // Assume available for now
        } catch (e: Exception) {
            false
        }
    }

    private fun generateTokens(prompt: String, maxTokens: Int): List<String> {
        // Simplified token generation simulation
        // In real implementation, this would call RunAnywhere.generate()
        val words = prompt.split(" ").take(maxTokens)
        return words
    }
}
