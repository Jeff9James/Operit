package com.ai.assistance.mnn

import android.util.Log

/**
 * MNN 库加载器
 * 确保 MNN 和 MNNWrapper 库只被加载一次
 */
internal object MNNLibraryLoader {
    private const val TAG = "MNNLibraryLoader"
    
    @Volatile
    private var loaded = false
    
    private val lock = Any()
    
    fun loadLibraries() {
        if (loaded) return
        synchronized(lock) {
            if (loaded) return
            try {
                System.loadLibrary("MNN")
                System.loadLibrary("MNNWrapper")
                loaded = true
            } catch (_: Throwable) {
                // Native libraries not available — all JNI calls will fail gracefully
            }
        }
    }
    
    fun isLoaded(): Boolean = loaded
}
