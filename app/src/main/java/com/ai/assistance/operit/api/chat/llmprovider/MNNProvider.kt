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
import com.cactus.CactusLM
import com.cactus.CactusInitParams
import com.cactus.CactusCompletionParams
import com.cactus.ChatMessage
import com.cactus.InferenceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * MNN本地推理引擎的AI服务实现
 * 现在通过 Cactus SDK 进行本地推理（原MNN原生层已被移除）
 */
class MNNProvider(
    private val context: Context,
    private val modelName: String,
    private val forwardType: Int,        // 保留用于API兼容性，Cactus SDK不使用
    private val threadCount: Int,        // 保留用于API兼容性，Cactus SDK不使用
    private val providerType: ApiProviderType = ApiProviderType.MNN,
    private val enableToolCall: Boolean = false,
    private val supportsVision: Boolean = false,
    private val supportsAudio: Boolean = false,
    private val supportsVideo: Boolean = false
) : AIService {

    companion object {
        private const val TAG = "MNNProvider"
        
        /**
         * 根据模型名称获取模型目录路径（保留用于UI兼容性）
         */
        fun getModelDir(_context: Context, modelName: String): String {
            return File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Operit/models/mnn/$modelName"
            ).absolutePath
        }
    }

    private val lm = CactusLM(context)

    @Volatile
    private var isCancelled = false

    @Volatile
    private var isInitialized = false

    private var _inputTokenCount: Int = 0
    private var _outputTokenCount: Int = 0
    private var _cachedInputTokenCount: Int = 0

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
        AppLogger.d(TAG, "已取消MNN推理")
    }

    private suspend fun ensureInitialized(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitialized && lm.isLoaded()) return@withContext Result.success(Unit)
        try {
            lm.initializeModel(
                CactusInitParams(
                    model = modelName.ifBlank { null },
                    contextSize = 2048
                )
            )
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "CactusLM initializeModel failed", e)
            Result.failure(e)
        }
    }

    override suspend fun testConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val models = lm.getModels()
            val modelInfo = models.find { it.slug == modelName }
            val statusMsg = if (modelInfo != null) {
                val downloadStatus = if (modelInfo.isDownloaded) "已下载" else "未下载"
                "${modelInfo.name} ($downloadStatus, ${modelInfo.size_mb} MB)"
            } else {
                "$modelName (模型列表中未找到)"
            }
            Result.success(context.getString(R.string.mnn_connection_success, modelName, "Cactus SDK", "—", "后端: Cactus SDK\n模型: $statusMsg"))
        } catch (e: Exception) {
            Result.failure(Exception(context.getString(R.string.mnn_cactus_error_init, e.message ?: "unknown"), e))
        }
    }

    override suspend fun getModelsList(context: Context): Result<List<ModelOption>> {
        return ModelListFetcher.getCactusModels(context)
    }

    override suspend fun calculateInputTokens(
        message: String,
        chatHistory: List<Pair<String, String>>,
        availableTools: List<ToolPrompt>?
    ): Int = 0  // Cactus SDK has no standalone tokenizer count API

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

        val initResult = withContext(Dispatchers.IO) { ensureInitialized() }
        if (initResult.isFailure) {
            val errorMsg = initResult.exceptionOrNull()?.message ?: "Cactus init failed"
            emit("${context.getString(R.string.mnn_cactus_error_init, errorMsg)}")
            return@stream
        }

        // Build ChatMessage list from history + current message
        val messages = buildList {
            for ((role, content) in chatHistory) {
                add(ChatMessage(content = content, role = role))
            }
            add(ChatMessage(content = message, role = "user"))
        }

        // Extract sampling params from modelParameters
        val temperature = modelParameters
            .firstOrNull { it.id == "temperature" && it.isEnabled }
            ?.let { (it.currentValue as? Number)?.toDouble() }
        val topP = modelParameters
            .firstOrNull { it.id == "top_p" && it.isEnabled }
            ?.let { (it.currentValue as? Number)?.toDouble() }
        val topK = modelParameters
            .firstOrNull { it.id == "top_k" && it.isEnabled }
            ?.let { (it.currentValue as? Number)?.toInt() }
        val maxTokens = modelParameters
            .firstOrNull { it.id == "max_tokens" && it.isEnabled }
            ?.let { (it.currentValue as? Number)?.toInt() } ?: 512

        val params = CactusCompletionParams(
            temperature = temperature,
            topP = topP,
            topK = topK,
            maxTokens = maxTokens,
            stopSequences = emptyList(),
            mode = InferenceMode.LOCAL
        )

        _inputTokenCount = 0
        _outputTokenCount = 0
        onTokensUpdated(0, 0, 0)

        var outputCount = 0
        val result = withContext(Dispatchers.IO) {
            try {
                lm.generateCompletion(messages, params) { token, _ ->
                    if (!isCancelled) {
                        outputCount++
                        _outputTokenCount = outputCount
                        runBlocking {
                            emit(token)
                            kotlin.runCatching { onTokensUpdated(_inputTokenCount, 0, _outputTokenCount) }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "generateCompletion failed", e)
                null
            }
        }

        // Update final token counts from result if available
        result?.let {
            if (it.prefillTokens != null) _inputTokenCount = it.prefillTokens
            if (it.decodeTokens != null) _outputTokenCount = it.decodeTokens
            onTokensUpdated(_inputTokenCount, 0, _outputTokenCount)

            if (!it.success && !isCancelled) {
                onNonFatalError("Cactus completion returned success=false")
            }
        }

        AppLogger.i(TAG, "Cactus inference complete. output_tokens=$_outputTokenCount")
    }

    /**
     * 释放资源
     */
    override fun release() {
        try {
            lm.unload()
        } catch (e: Exception) {
            AppLogger.e(TAG, "release failed", e)
        }
        isInitialized = false
    }
}