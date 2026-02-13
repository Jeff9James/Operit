package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import android.os.Environment
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Centralized manager for SDK-based model operations.
 * Handles model listing, downloading, and management for both
 * Runanywhere and Cactus compute SDKs.
 */
object SdkModelManager {
    private const val TAG = "SdkModelManager"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Represents a downloadable model with full metadata
     */
    data class SdkModel(
        val id: String,
        val name: String,
        val downloadUrl: String,
        val sizeBytes: Long = 0,
        val description: String = "",
        val isDownloaded: Boolean = false,
        val localPath: String? = null
    )

    /**
     * Download progress state
     */
    data class DownloadProgress(
        val modelId: String,
        val progress: Float, // 0.0 to 1.0
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val state: DownloadState,
        val error: String? = null
    )

    enum class DownloadState {
        PENDING,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    // =====================================================
    // RUNANYWHERE SDK MODELS
    // Based on Runanywhere SDK documentation
    // =====================================================
    
    fun getRunanywhereModels(): List<SdkModel> = listOf(
        // LLM Models
        SdkModel(
            id = "smollm2-360m-instruct-q8_0",
            name = "SmolLM2 360M Instruct Q8_0",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf",
            sizeBytes = 400_000_000L,
            description = "Small but capable instruction-tuned model (~400MB)"
        ),
        SdkModel(
            id = "smollm2-135m-instruct-q8_0",
            name = "SmolLM2 135M Instruct Q8_0",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q8_0.gguf",
            sizeBytes = 150_000_000L,
            description = "Smallest SmolLM2 variant (~150MB)"
        ),
        SdkModel(
            id = "smollm2-1.7b-instruct-q4_k_m",
            name = "SmolLM2 1.7B Instruct Q4_K_M",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
            sizeBytes = 1_000_000_000L,
            description = "Larger SmolLM2 model (~1GB)"
        ),
        SdkModel(
            id = "qwen2.5-0.5b-instruct-q8_0",
            name = "Qwen2.5 0.5B Instruct Q8_0",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q8_0.gguf",
            sizeBytes = 600_000_000L,
            description = "Qwen2.5 small model (~600MB)"
        ),
        SdkModel(
            id = "qwen2.5-1.5b-instruct-q4_k_m",
            name = "Qwen2.5 1.5B Instruct Q4_K_M",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            sizeBytes = 1_000_000_000L,
            description = "Qwen2.5 medium model (~1GB)"
        ),
        SdkModel(
            id = "phi-3.5-mini-instruct-q4_k_m",
            name = "Phi-3.5 Mini Instruct Q4_K_M",
            downloadUrl = "https://huggingface.co/microsoft/Phi-3.5-mini-instruct-gguf/resolve/main/Phi-3.5-mini-instruct-q4.gguf",
            sizeBytes = 2_300_000_000L,
            description = "Microsoft Phi-3.5 Mini (~2.3GB)"
        ),
        SdkModel(
            id = "gemma-2-2b-it-q4_k_m",
            name = "Gemma-2 2B IT Q4_K_M",
            downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-q4_k_m.gguf",
            sizeBytes = 1_600_000_000L,
            description = "Google Gemma-2 2B (~1.6GB)"
        ),
        // STT Models
        SdkModel(
            id = "sherpa-onnx-whisper-tiny.en",
            name = "Whisper Tiny EN (STT)",
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.en.tar.bz2",
            sizeBytes = 80_000_000L,
            description = "English speech-to-text (~80MB)"
        ),
        SdkModel(
            id = "moonshine-tiny",
            name = "Moonshine Tiny (STT)",
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-moonshine-tiny.tar.bz2",
            sizeBytes = 50_000_000L,
            description = "Lightweight STT model (~50MB)"
        )
    )

    // =====================================================
    // CACTUS COMPUTE SDK MODELS
    // Based on Cactus SDK documentation
    // Using verified HuggingFace GGUF model URLs
    // =====================================================
    
    fun getCactusModels(): List<SdkModel> = listOf(
        // Qwen2.5 series - verified URLs
        SdkModel(
            id = "Qwen/Qwen2.5-0.5B-Instruct",
            name = "Qwen2.5 0.5B Instruct",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            sizeBytes = 400_000_000L,
            description = "Small Qwen2.5 instruct model (~400MB)"
        ),
        SdkModel(
            id = "Qwen/Qwen2.5-1.5B-Instruct",
            name = "Qwen2.5 1.5B Instruct",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            sizeBytes = 1_100_000_000L,
            description = "Medium Qwen2.5 instruct model (~1.1GB)"
        ),
        SdkModel(
            id = "Qwen/Qwen2.5-3B-Instruct",
            name = "Qwen2.5 3B Instruct",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
            sizeBytes = 2_000_000_000L,
            description = "Large Qwen2.5 instruct model (~2GB)"
        ),
        // SmolLM2 series - verified URLs
        SdkModel(
            id = "HuggingFaceTB/SmolLM2-135M-Instruct",
            name = "SmolLM2 135M Instruct",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_k_m.gguf",
            sizeBytes = 150_000_000L,
            description = "Smallest SmolLM2 model (~150MB)"
        ),
        SdkModel(
            id = "HuggingFaceTB/SmolLM2-360M-Instruct",
            name = "SmolLM2 360M Instruct",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
            sizeBytes = 300_000_000L,
            description = "Small SmolLM2 model (~300MB)"
        ),
        SdkModel(
            id = "HuggingFaceTB/SmolLM2-1.7B-Instruct",
            name = "SmolLM2 1.7B Instruct",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
            sizeBytes = 1_100_000_000L,
            description = "Large SmolLM2 model (~1.1GB)"
        ),
        // Phi-3 series - verified URLs
        SdkModel(
            id = "microsoft/Phi-3.5-mini-instruct",
            name = "Phi-3.5 Mini Instruct",
            downloadUrl = "https://huggingface.co/microsoft/Phi-3.5-mini-instruct-gguf/resolve/main/Phi-3.5-mini-instruct-q4.gguf",
            sizeBytes = 2_300_000_000L,
            description = "Microsoft Phi-3.5 Mini (~2.3GB)"
        ),
        // Gemma-2 series - verified URLs
        SdkModel(
            id = "google/gemma-2-2b-it",
            name = "Gemma-2 2B IT",
            downloadUrl = "https://huggingface.co/google/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-q4_k_m.gguf",
            sizeBytes = 1_600_000_000L,
            description = "Google Gemma-2 2B (~1.6GB)"
        ),
        // Llama-3.2 series - verified URLs
        SdkModel(
            id = "meta-llama/Llama-3.2-1B-Instruct",
            name = "Llama-3.2 1B Instruct",
            downloadUrl = "https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            sizeBytes = 800_000_000L,
            description = "Meta Llama-3.2 1B (~800MB)"
        ),
        SdkModel(
            id = "meta-llama/Llama-3.2-3B-Instruct",
            name = "Llama-3.2 3B Instruct",
            downloadUrl = "https://huggingface.co/meta-llama/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            sizeBytes = 2_000_000_000L,
            description = "Meta Llama-3.2 3B (~2GB)"
        )
    )

    /**
     * Get the models directory for a specific provider
     */
    fun getModelsDir(provider: String): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Operit/models/$provider"
        )
    }

    /**
     * Check if a model is already downloaded
     */
    fun isModelDownloaded(context: Context, provider: String, modelId: String): Boolean {
        val modelsDir = getModelsDir(provider)
        val modelFile = File(modelsDir, getSafeModelFileName(modelId))
        return modelFile.exists() && modelFile.length() > 0
    }

    /**
     * Get the local path for a downloaded model
     */
    fun getModelLocalPath(provider: String, modelId: String): String {
        val modelsDir = getModelsDir(provider)
        return File(modelsDir, getSafeModelFileName(modelId)).absolutePath
    }

    /**
     * Convert a model ID to a safe file name
     */
    private fun getSafeModelFileName(modelId: String): String {
        return modelId
            .replace("/", "_")
            .replace(":", "_")
            .replace("?", "_")
            .let { if (it.endsWith(".gguf")) it else "$it.gguf" }
    }

    /**
     * Get list of downloaded models for a provider
     */
    fun getDownloadedModels(provider: String): List<String> {
        val modelsDir = getModelsDir(provider)
        if (!modelsDir.exists()) return emptyList()
        
        return modelsDir.listFiles { file -> 
            file.isFile && file.name.endsWith(".gguf") 
        }?.map { file ->
            file.name.removeSuffix(".gguf")
        } ?: emptyList()
    }

    /**
     * Get all available models for a provider with download status
     */
    fun getAvailableModels(context: Context, provider: String): List<SdkModel> {
        val models = when (provider.lowercase()) {
            "runanywhere" -> getRunanywhereModels()
            "cactus" -> getCactusModels()
            else -> emptyList()
        }
        
        return models.map { model ->
            model.copy(
                isDownloaded = isModelDownloaded(context, provider, model.id),
                localPath = if (isModelDownloaded(context, provider, model.id)) {
                    getModelLocalPath(provider, model.id)
                } else null
            )
        }
    }

    /**
     * Download a model with progress tracking
     * Returns a Flow of DownloadProgress
     */
    fun downloadModel(
        context: Context,
        provider: String,
        modelId: String
    ): Flow<DownloadProgress> = flow {
        val models = when (provider.lowercase()) {
            "runanywhere" -> getRunanywhereModels()
            "cactus" -> getCactusModels()
            else -> emptyList()
        }
        
        val model = models.find { it.id == modelId }
            ?: throw IllegalArgumentException("Model not found: $modelId")
        
        val modelsDir = getModelsDir(provider)
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        
        val modelFile = File(modelsDir, getSafeModelFileName(modelId))
        
        // Emit pending state
        emit(DownloadProgress(
            modelId = modelId,
            progress = 0f,
            bytesDownloaded = 0,
            totalBytes = model.sizeBytes,
            state = DownloadState.PENDING
        ))
        
        try {
            AppLogger.d(TAG, "Starting download for model: $modelId from ${model.downloadUrl}")
            
            val request = Request.Builder()
                .url(model.downloadUrl)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Download failed: HTTP ${response.code}")
            }
            
            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) contentLength else model.sizeBytes
            
            var bytesDownloaded = 0L
            val buffer = ByteArray(8192)
            
            FileOutputStream(modelFile).use { output ->
                body.byteStream().use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        
                        // Emit progress every 100KB
                        if (bytesDownloaded % 102400 == 0L || bytesRead == -1) {
                            emit(DownloadProgress(
                                modelId = modelId,
                                progress = bytesDownloaded.toFloat() / totalBytes,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                state = DownloadState.DOWNLOADING
                            ))
                        }
                    }
                }
            }
            
            response.close()
            
            // Emit completion
            emit(DownloadProgress(
                modelId = modelId,
                progress = 1f,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                state = DownloadState.COMPLETED
            ))
            
            AppLogger.d(TAG, "Download completed for model: $modelId")
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Download failed for model: $modelId - ${e.message}", e)
            
            // Delete partial file
            if (modelFile.exists()) {
                modelFile.delete()
            }
            
            emit(DownloadProgress(
                modelId = modelId,
                progress = 0f,
                bytesDownloaded = 0,
                totalBytes = model.sizeBytes,
                state = DownloadState.FAILED,
                error = e.message
            ))
        }
    }

    /**
     * Delete a downloaded model
     */
    fun deleteModel(provider: String, modelId: String): Boolean {
        val modelsDir = getModelsDir(provider)
        val modelFile = File(modelsDir, getSafeModelFileName(modelId))
        return if (modelFile.exists()) {
            modelFile.delete()
        } else {
            false
        }
    }

    /**
     * Get model options for UI (converts SdkModel to ModelOption)
     */
    fun getModelOptions(context: Context, provider: String): List<ModelOption> {
        return getAvailableModels(context, provider).map { model ->
            ModelOption(
                id = model.id,
                name = "${model.name}${if (model.isDownloaded) " ✓" else " (Download)"}"
            )
        }
    }

    /**
     * Get the default model ID for a provider
     */
    fun getDefaultModelId(provider: String): String {
        return when (provider.lowercase()) {
            "runanywhere" -> "smollm2-360m-instruct-q8_0"
            "cactus" -> "Qwen/Qwen2.5-0.5B-Instruct"
            else -> ""
        }
    }
    
    /**
     * Check if a provider is an SDK-based provider (doesn't require API key)
     */
    fun isSdkProvider(providerType: com.ai.assistance.operit.data.model.ApiProviderType): Boolean {
        return providerType == com.ai.assistance.operit.data.model.ApiProviderType.CACTUS ||
               providerType == com.ai.assistance.operit.data.model.ApiProviderType.RUNANYWHERE
    }
    
    /**
     * Check if an SDK provider has at least one downloaded model
     */
    fun hasDownloadedModel(context: Context, providerType: com.ai.assistance.operit.data.model.ApiProviderType): Boolean {
        val providerName = when (providerType) {
            com.ai.assistance.operit.data.model.ApiProviderType.CACTUS -> "cactus"
            com.ai.assistance.operit.data.model.ApiProviderType.RUNANYWHERE -> "runanywhere"
            else -> return false
        }
        
        val models = getAvailableModels(context, providerName)
        return models.any { it.isDownloaded }
    }
}
