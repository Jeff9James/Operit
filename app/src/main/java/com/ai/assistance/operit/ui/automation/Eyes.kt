package com.ai.assistance.operit.ui.automation

import android.graphics.Bitmap
import com.ai.assistance.operit.services.OperitAccessibilityService

/**
 * Eyes - High-level API for screen reading
 * Provides screenshot capture and UI hierarchy access
 */
class Eyes(private val service: OperitAccessibilityService) {
    
    companion object {
        /**
         * Get singleton instance
         */
        fun getInstance(): Eyes? {
            val service = OperitAccessibilityService.getInstance()
            return service?.let { Eyes(it) }
        }
    }
    
    /**
     * Capture screenshot
     */
    suspend fun captureScreenshot(): Bitmap? {
        return service.takeScreenshot()
    }
    
    /**
     * Get UI hierarchy as XML string
     */
    fun getUIHierarchy(): String {
        return service.getUIHierarchyXml()
    }
    
    /**
     * Get simplified UI hierarchy
     */
    fun getSimplifiedHierarchy(): String {
        return service.getSimplifiedUIHierarchy()
    }
    
    /**
     * Get interactive elements
     */
    fun getInteractiveElements(): Map<Int, InteractiveElement> {
        return service.getInteractiveElements()
    }
    
    /**
     * Get current activity name
     */
    fun getCurrentActivity(): String? {
        return service.getCurrentActivity()
    }
    
    /**
     * Get screen size
     */
    fun getScreenSize(): Pair<Int, Int> {
        return service.getScreenSize()
    }
    
    /**
     * Check if service is connected
     */
    fun isConnected(): Boolean {
        return service.isServiceConnected()
    }
    
    /**
     * Get screen as base64 PNG
     */
    suspend fun getScreenAsBase64(): String? {
        val bitmap = captureScreenshot() ?: return null
        
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }
}
