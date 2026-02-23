package com.ai.assistance.llama

/**
 * Native JNI interface for llama.cpp.
 * All external functions will throw UnsatisfiedLinkError at call-time if the
 * native library is not available. The init block has been removed to prevent
 * class-load-time crashes.
 */
object LlamaNative {

    // No init block - library loading is deferred to LlamaLibraryLoader.loadLibraries()
    // which is called lazily and swallows errors gracefully.

    @JvmStatic external fun nativeIsAvailable(): Boolean

    @JvmStatic external fun nativeGetUnavailableReason(): String

    @JvmStatic external fun nativeCreateSession(pathModel: String, nThreads: Int, nCtx: Int): Long

    @JvmStatic external fun nativeReleaseSession(sessionPtr: Long)

    @JvmStatic external fun nativeCancel(sessionPtr: Long)

    @JvmStatic external fun nativeCountTokens(sessionPtr: Long, text: String): Int

    @JvmStatic
    external fun nativeSetSamplingParams(
        sessionPtr: Long,
        temperature: Float,
        topP: Float,
        topK: Int,
        repetitionPenalty: Float,
        frequencyPenalty: Float,
        presencePenalty: Float,
        penaltyLastN: Int
    ): Boolean

    @JvmStatic
    external fun nativeApplyChatTemplate(
        sessionPtr: Long,
        roles: Array<String>,
        contents: Array<String>,
        addAssistant: Boolean
    ): String?

    @JvmStatic
    external fun nativeGenerateStream(
        sessionPtr: Long,
        prompt: String,
        maxTokens: Int,
        callback: GenerationCallback
    ): Boolean

    interface GenerationCallback {
        fun onToken(token: String): Boolean
    }
}
