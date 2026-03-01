package com.ai.assistance.llama

import android.util.Log

internal object LlamaLibraryLoader {
    private const val TAG = "LlamaLibraryLoader"
    
    @Volatile
    var loaded = false
        private set

    private val lock = Any()

    fun loadLibraries() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            try {
                System.loadLibrary("LlamaWrapper")
                loaded = true
                Log.i(TAG, "LlamaWrapper library loaded successfully")
            } catch (e: Throwable) {
                // .so not available — all JNI calls will fail gracefully via isAvailable()
                Log.w(TAG, "LlamaWrapper library not available: ${e.message}")
                // Don't set loaded = true since it failed
            }
        }
    }
}
