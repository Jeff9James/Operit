package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.util.AppLogger
import com.cactus.CactusLM
import com.cactus.CactusModelManager as SdkCactusModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper object for managing Cactus SDK models.
 * Provides download, status checking, and model listing functionality.
 */
object CactusModelManager {
    private const val TAG = "CactusModelManager"

    /**
     * Download a Cactus model by slug.
     * Note: The Cactus SDK downloadModel API does not provide progress callbacks.
     * This function handles the download and returns success/failure.
     *
     * @param context Android context
     * @param slug Model slug (e.g., "qwen3-0.6")
     * @return Result indicating success or failure
     */
    suspend fun downloadModel(
        context: Context,
        slug: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Starting download for model: $slug")
            val lm = CactusLM()
            lm.downloadModel(slug)
            AppLogger.d(TAG, "Download completed for model: $slug")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Download failed for model: $slug", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a model is already downloaded locally.
     *
     * @param context Android context
     * @param slug Model slug to check
     * @return true if the model is downloaded, false otherwise
     */
    fun isModelDownloaded(context: Context, slug: String): Boolean {
        return SdkCactusModelManager.isModelDownloaded(slug)
    }

    /**
     * Get list of locally cached/downloaded model slugs.
     *
     * @param context Android context
     * @return List of downloaded model slugs
     */
    fun getLocalModels(context: Context): List<String> {
        return SdkCactusModelManager.getDownloadedModels()
    }

    /**
     * Get detailed list of locally cached models as ModelOption objects.
     * Combines local models with available models from Cactus SDK to get metadata.
     *
     * @param context Android context
     * @return Result containing list of ModelOption for downloaded models
     */
    suspend fun getLocalModelOptions(context: Context): Result<List<ModelOption>> = withContext(Dispatchers.IO) {
        try {
            val downloadedSlugs = SdkCactusModelManager.getDownloadedModels()
            if (downloadedSlugs.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // Get available models from SDK to get metadata (name, size, etc.)
            val lm = CactusLM()
            val availableModels = lm.getModels()

            val localModels = downloadedSlugs.mapNotNull { slug ->
                val modelInfo = availableModels.find { it.slug == slug }
                if (modelInfo != null) {
                    ModelOption(
                        id = modelInfo.slug,
                        name = "${modelInfo.name} (${modelInfo.size_mb} MB)"
                    )
                } else {
                    // Model is downloaded but not in available list (older model, etc.)
                    ModelOption(
                        id = slug,
                        name = slug
                    )
                }
            }

            Result.success(localModels)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to get local model options", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a downloaded model to free storage.
     *
     * @param context Android context
     * @param slug Model slug to delete
     * @return true if deleted successfully, false if model not found
     */
    fun deleteModel(context: Context, slug: String): Boolean {
        return SdkCactusModelManager.deleteModel(slug)
    }

    /**
     * Get the models directory path for debugging.
     *
     * @param context Android context
     * @return Absolute path to models storage directory
     */
    fun getModelsDirectory(context: Context): String {
        return SdkCactusModelManager.getModelsDirectory()
    }
}
