package com.ai.assistance.operit.ui.automation

import android.content.Context
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Eyes API - Screen reading wrapper
 * Provides high-level methods for screen perception
 * Based on Blurr's Eyes implementation
 */
class Eyes(private val context: Context) {
    
    private val service: ScreenInteractionService?
        get() = ScreenInteractionService.instance
    
    /**
     * Capture screenshot of current screen
     * Requires API level R (30) or higher
     */
    suspend fun openEyes(): Bitmap? = withContext(Dispatchers.IO) {
        service?.captureScreenshot()
    }
    
    /**
     * Dump current UI layout as pure XML
     */
    suspend fun openPureXMLEyes(): String = withContext(Dispatchers.IO) {
        service?.dumpWindowHierarchy(pureXML = true) ?: ""
    }
    
    /**
     * Dump current UI layout as readable markdown format
     */
    suspend fun openXMLEyes(): String = withContext(Dispatchers.IO) {
        service?.dumpWindowHierarchy(pureXML = false) ?: ""
    }
    
    /**
     * Check if keyboard is currently available for typing
     */
    fun getKeyBoardStatus(): Boolean {
        return service?.isKeyboardOpened() ?: false
    }
    
    /**
     * Get raw screen data including UI hierarchy and scroll information
     */
    suspend fun getRawScreenData(): RawScreenData? = withContext(Dispatchers.IO) {
        service?.getScreenAnalysisData(getAll = false)
    }
    
    /**
     * Get all raw screen data from all windows
     */
    suspend fun getAllRawScreenData(): RawScreenData? = withContext(Dispatchers.IO) {
        service?.getAllScreenAnalysisData()
    }
    
    /**
     * Get current foreground activity name
     */
    fun getCurrentActivityName(): String? {
        return service?.getCurrentActivityName()
    }
    
    /**
     * Get current package name
     */
    fun getCurrentPackageName(): String? {
        return service?.getCurrentPackageName()
    }
    
    /**
     * Get UI hierarchy signature for change detection
     */
    fun getWindowHierarchySignature(): String {
        return service?.getWindowHierarchySignature() ?: ""
    }
}

/**
 * Extension function to get Eyes instance
 */
fun Context.getEyes(): Eyes = Eyes(this)
