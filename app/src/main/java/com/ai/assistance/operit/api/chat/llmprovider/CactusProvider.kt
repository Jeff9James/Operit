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
import com.cactus.CactusCompletionParams
import com.cactus.CactusInitParams
import com.cactus.CactusLM
import com.cactus.ChatMessage
import com.cactus.InferenceMode
import com.cactus.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Cactus Compute SDK provider for on-device LLM inference.
 * Supports Qwen2/Qwen3, Gemma2/Gemma3, LFM2/LFM2.5, SmolLM2 models.
 * Features: Local inference, Remote inference (cloud handoff), Function calling, Vision/Multimodal, Embeddings.
 */
class CactusProvider(
    private val context: Context,
    private val modelName: String,
    private val threadCount: Int = 4,
    private val contextSize: Int = 2048,
    private val inferenceMode: String = "LOCAL_FIRST",
    private val cactusToken: String = "",
    private val providerType: ApiProviderType = ApiProviderType.CACTUS
) : AIService {

    companion object {
        private const val TAG = "CactusProvider"

        fun getModelsDir(): File {
            return File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Operit/models/cactus"
            )
        }

        fun getDefaultModels(): List<ModelOption> = listOf(
            ModelOption("qwen3-0.6", "Qwen3 0.6B (Default)", true),
            ModelOption("qwen2.5-0.5b", "Qwen2.5 0.5B", true),
            ModelOption("gemma3-270m", "Gemma3 270M", true),
            ModelOption("gemma3-1b", "Gemma3 1B", true),
            ModelOption("smollm2-360m", "SmolLM2 360M", true),
            ModelOption("lfm2-1b", "LFM2 1B", false),
            ModelOption("lfm2-3b", "LFM2 3B", false)
        )
    }

    private var _inputTokenCount: Int = 0
    private var _outputTokenCount: Int = 0
    private var _cachedInputTokenCount: Int = 0

    @Volatile
    private var isCancelled = false

    private val sessionLock = Any()
    private var cactusLM: CactusLM? = null

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
            cactusLM?.unload()
            cactusLM = null
        }
    }

    override suspend fun getModelsList(context: Context): Result<List<ModelOption>> {
        return Result.success(getDefaultModels())
    }

    override suspend fun testConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val lm = getOrCreateSession()
            if (lm.isLoaded()) {
                Result.success("Cactus SDK is available. Model '$modelName' is loaded.")
            } else {
                Result.success("Cactus SDK is available. Model '$modelName' not loaded yet.")
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
        // Cactus SDK doesn't provide token counting, estimate based on character count
        return withContext(Dispatchers.IO) {
            val totalChars = chatHistory.sumOf { it.second.length } + message.length
            totalChars / 4 // Rough estimate: 4 chars per token
        }
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
            val lm = withContext(Dispatchers.IO) {
                getOrCreateSession()
            }

            // Build chat messages
            val messages = buildMessages(chatHistory, message)

            // Build completion params
            val params = buildCompletionParams(modelParameters, availableTools)

            AppLogger.d(TAG, "Starting Cactus inference with model: $modelName")

            val result = withContext(Dispatchers.IO) {
                if (stream) {
                    lm.generateCompletion(
                        messages = messages,
                        params = params,
                        onToken = { token, _ ->
                            if (!isCancelled) {
                                _outputTokenCount++
                                runBlocking { emit(token) }
                                kotlinx.coroutines.runBlocking {
                                    onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)
                                }
                            }
                        }
                    )
                } else {
                    lm.generateCompletion(messages = messages, params = params)
                }
            }

            result?.let { completionResult ->
                if (completionResult.success) {
                    _outputTokenCount = completionResult.decodeTokens ?: _outputTokenCount
                    _inputTokenCount = completionResult.prefillTokens ?: _inputTokenCount
                    onTokensUpdated(_inputTokenCount, _cachedInputTokenCount, _outputTokenCount)

                    // Handle tool calls if any
                    if (completionResult.toolCalls.isNotEmpty()) {
                        val toolCallXml = formatToolCalls(completionResult.toolCalls)
                        emit(toolCallXml)
                    }
                } else {
                    val errorMsg = completionResult.response ?: "Unknown error"
                    emit("\n\n[Error]: $errorMsg")
                }
            } ?: emit("\n\n[Error]: Cactus completion returned null")

        } catch (e: Exception) {
            AppLogger.e(TAG, "Cactus inference failed", e)
            emit("\n\n[Error]: ${e.message ?: "Unknown error"}")
        }
    }

    private fun getOrCreateSession(): CactusLM {
        return synchronized(sessionLock) {
            cactusLM?.let { return it }

            val lm = CactusLM()
            val modelsDir = getModelsDir()
            val modelFile = File(modelsDir, "$modelName.bin")

            // Download model if needed (simplified - in real app would check if exists)
            kotlinx.coroutines.runBlocking {
                try {
                    lm.downloadModel(modelName)
                    lm.initializeModel(
                        CactusInitParams(
                            model = modelName,
                            contextSize = contextSize
                        )
                    )
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Model download/initialization: ${e.message}")
                }
            }
            cactusLM = lm
            lm
        }
    }

    private fun buildMessages(
        chatHistory: List<Pair<String, String>>,
        message: String
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        // Add chat history
        for ((role, content) in chatHistory) {
            val standardRole = when (role.lowercase()) {
                "ai", "assistant" -> "assistant"
                "user" -> "user"
                "system" -> "system"
                else -> "user"
            }
            messages.add(ChatMessage(content = content, role = standardRole))
        }

        // Add current message
        messages.add(ChatMessage(content = message, role = "user"))

        return messages
    }

    private fun buildCompletionParams(
        modelParameters: List<ModelParameter<*>>,
        availableTools: List<ToolPrompt>?
    ): CactusCompletionParams {
        val temperature = modelParameters
            .firstOrNull { it.id == "temperature" && it.isEnabled }
            ?.let { (it.currentValue as? Number)?.toDouble() }
            ?: 0.7

        val topP = modelParameters
            .firstOrNull { it.id == "top_p" && it.isEnabled }
            ?.let { (it.currentValue as? Number)?.toDouble() }
            ?: 0.95

        val topK = modelParameters
            .firstOrNull { it.id == "top_k" && it.isEnabled }
            ?.let { (it.currentValue as? Number)?.toInt() }
            ?: 0

        val maxTokens = modelParameters
            .find { it.name == "max_tokens" }
            ?.let { (it.currentValue as? Number)?.toInt() }
            ?: 2048

        val stopSequences = listOf("<|im_end|>", "<end_of_turn>")

        // Convert tools if available
        val tools = availableTools?.map { tool ->
            com.cactus.CactusTool(
                function = com.cactus.CactusFunction(
                    name = tool.name,
                    description = tool.description,
                    parameters = com.cactus.ToolParametersSchema(
                        properties = tool.parameters.mapValues { param ->
                            com.cactus.ToolParameter(
                                type = param.value.type ?: "string",
                                description = param.value.description ?: "",
                                required = param.value.required
                            )
                        },
                        required = tool.parameters.filter { it.value.required }.map { it.key }
                    )
                )
            )
        } ?: emptyList()

        // Parse inference mode
        val mode = when (inferenceMode.uppercase()) {
            "LOCAL" -> InferenceMode.LOCAL
            "REMOTE" -> InferenceMode.REMOTE
            "LOCAL_FIRST" -> InferenceMode.LOCAL_FIRST
            "REMOTE_FIRST" -> InferenceMode.REMOTE_FIRST
            else -> InferenceMode.LOCAL_FIRST
        }

        return CactusCompletionParams(
            temperature = temperature,
            topK = topK,
            topP = topP,
            maxTokens = maxTokens,
            stopSequences = stopSequences,
            tools = tools,
            mode = mode,
            cactusToken = cactusToken.takeIf { it.isNotBlank() }
        )
    }

    private fun formatToolCalls(toolCalls: List<ToolCall>): String {
        return toolCalls.joinToString("\n") { toolCall ->
            val argsJson = toolCall.arguments.entries.joinToString(",") { "\"${it.key}\": \"${it.value}\"" }
            "<tool_call>\n<tool name=\"${toolCall.name}\">\n<arguments>{$argsJson}</arguments>\n</tool>\n</tool_call>"
        }
    }
}
