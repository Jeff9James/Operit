package com.ai.assistance.operit.ui.automation

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Operit Screen Interaction Service
 * Core AccessibilityService that provides UI automation capabilities
 * Based on Blurr's ScreenInteractionService implementation
 */
class ScreenInteractionService : AccessibilityService() {

    companion object {
        private const val TAG = "ScreenInteractionService"
        
        @Volatile
        var instance: ScreenInteractionService? = null
            private set
            
        var showDebugTap: Boolean = false
    }
    
    private var windowManager: WindowManager? = null
    private var currentActivityName: String? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        AppLogger.d(TAG, "ScreenInteractionService connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentActivityName = event.packageName?.toString()
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Handle content changes if needed
            }
        }
    }
    
    override fun onInterrupt() {
        AppLogger.d(TAG, "Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        AppLogger.d(TAG, "ScreenInteractionService destroyed")
    }
    
    // ==================== UI Hierarchy Methods ====================
    
    /**
     * Dump the current UI hierarchy as XML
     * @param pureXML If true, returns pure XML; otherwise returns readable format
     */
    suspend fun dumpWindowHierarchy(pureXML: Boolean = false): String = withContext(Dispatchers.IO) {
        val rootNode = rootInActiveWindow ?: return@withContext ""
        
        try {
            if (pureXML) {
                dumpNodeToXml(rootNode)
            } else {
                val elements = mutableListOf<SimplifiedElement>()
                dumpNode(rootNode, elements, 0)
                formatElementsToReadable(elements)
            }
        } finally {
            rootNode.recycle()
        }
    }
    
    private fun dumpNodeToXml(node: AccessibilityNodeInfo): String {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()
        val rootElement = doc.createElement("node")
        doc.appendChild(rootElement)
        
        serializeNode(doc, rootElement, node)
        
        val transformer = TransformerFactory.newInstance().newTransformer()
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }
    
    private fun serializeNode(doc: org.w3c.dom.Document, parent: Element, node: AccessibilityNodeInfo) {
        val element = doc.createElement("node")
        
        // Add attributes
        node.text?.let { element.setAttribute("text", it.toString()) }
        node.resourceId?.let { element.setAttribute("resource-id", it.toString()) }
        node.className?.let { element.setAttribute("class", it.toString()) }
        node.packageName?.let { element.setAttribute("package", it.toString()) }
        node.contentDescription?.let { element.setAttribute("content-desc", it.toString()) }
        
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        element.setAttribute("bounds", "[$bounds.left,$bounds.top][$bounds.right,$bounds.bottom]")
        
        // Add state
        val states = mutableListOf<String>()
        if (node.isClickable) states.add("clickable")
        if (node.isLongClickable) states.add("long-clickable")
        if (node.isScrollable) states.add("scrollable")
        if (node.isEditable) states.add("editable")
        if (node.isCheckable) states.add("checkable")
        if (node.isChecked) states.add("checked")
        if (node.isEnabled) states.add("enabled")
        if (node.isFocused) states.add("focused")
        if (node.isSelected) states.add("selected")
        
        if (states.isNotEmpty()) {
            element.setAttribute("state", states.joinToString(","))
        }
        
        parent.appendChild(element)
        
        // Recursively process children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                serializeNode(doc, element, child)
                child.recycle()
            }
        }
    }
    
    private fun dumpNode(node: AccessibilityNodeInfo, elements: MutableList<SimplifiedElement>, depth: Int) {
        if (!isVisibleToUser(node)) return
        
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        
        val isImportant = isImportant(node)
        val isInteractive = isInteractive(node)
        
        if (isImportant || isInteractive) {
            elements.add(
                SimplifiedElement(
                    text = node.text?.toString() ?: "",
                    resourceId = node.resourceId?.toString() ?: "",
                    className = node.className?.toString() ?: "",
                    contentDescription = node.contentDescription?.toString() ?: "",
                    bounds = bounds,
                    depth = depth,
                    isClickable = node.isClickable,
                    isLongClickable = node.isLongClickable,
                    isScrollable = node.isScrollable,
                    isEditable = node.isEditable,
                    isCheckable = node.isCheckable,
                    isChecked = node.isChecked,
                    isEnabled = node.isEnabled,
                    isFocused = node.isFocused,
                    node = node
                )
            )
        }
        
        // Recursively process children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                dumpNode(child, elements, depth + 1)
                child.recycle()
            }
        }
    }
    
    private fun isVisibleToUser(node: AccessibilityNodeInfo): Boolean {
        return node.isVisibleToUser && node.visibility == AccessibilityNodeInfo.VISIBLE
    }
    
    private fun isImportant(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        val resourceId = node.resourceId?.toString()
        
        return !text.isNullOrBlank() || !contentDesc.isNullOrBlank() || 
               (!resourceId.isNullOrBlank() && resourceId != "null")
    }
    
    private fun isInteractive(node: AccessibilityNodeInfo): Boolean {
        return node.isClickable || node.isLongClickable || node.isCheckable || 
               node.isScrollable || node.isEditable || (node.isFocusable && node.isEnabled)
    }
    
    private fun formatElementsToReadable(elements: List<SimplifiedElement>): String {
        val sb = StringBuilder()
        sb.appendLine("## Screen Analysis")
        sb.appendLine()
        
        for ((index, element) in elements.withIndex()) {
            sb.appendLine("### Element $index")
            sb.appendLine("- Text: ${element.text}")
            sb.appendLine("- Resource ID: ${element.resourceId}")
            sb.appendLine("- Class: ${element.className}")
            sb.appendLine("- Content Description: ${element.contentDescription}")
            sb.appendLine("- Bounds: ${element.bounds}")
            
            val states = mutableListOf<String>()
            if (element.isClickable) states.add("clickable")
            if (element.isLongClickable) states.add("long-clickable")
            if (element.isScrollable) states.add("scrollable")
            if (element.isEditable) states.add("editable")
            if (element.isCheckable) states.add("checkable")
            if (element.isChecked) states.add("checked")
            if (element.isEnabled) states.add("enabled")
            if (element.isFocused) states.add("focused")
            
            if (states.isNotEmpty()) {
                sb.appendLine("- States: ${states.joinToString(", ")}")
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    /**
     * Get simplified XML representation of UI hierarchy
     * Used for detecting screen changes
     */
    fun getWindowHierarchySignature(): String {
        return try {
            val rootNode = rootInActiveWindow ?: return ""
            val sb = StringBuilder()
            buildSignature(rootNode, sb)
            rootNode.recycle()
            sb.toString()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting hierarchy signature", e)
            ""
        }
    }
    
    private fun buildSignature(node: AccessibilityNodeInfo, sb: StringBuilder) {
        sb.append(node.className)
        sb.append(node.resourceId)
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                buildSignature(child, sb)
                child.recycle()
            }
        }
    }
    
    // ==================== Screen Data Methods ====================
    
    /**
     * Get all screen analysis data including scroll info
     */
    suspend fun getAllScreenAnalysisData(): RawScreenData? = withContext(Dispatchers.IO) {
        getScreenAnalysisData(true)
    }
    
    /**
     * Get screen analysis data with scroll information
     */
    suspend fun getScreenAnalysisData(getAll: Boolean = false): RawScreenData? = withContext(Dispatchers.IO) {
        var rootNode: AccessibilityNodeInfo? = null
        var retries = 0
        val maxRetries = 3
        
        while (rootNode == null && retries < maxRetries) {
            rootNode = if (getAll) {
                // Get all windows and find the main app window
                windows.firstOrNull { window ->
                    window.title?.contains("operit", ignoreCase = true) == true ||
                    window.source?.packageName?.contains("operit", ignoreCase = true) == true
                }?.root ?: rootInActiveWindow
            } else {
                rootInActiveWindow
            }
            
            if (rootNode == null) {
                retries++
                kotlinx.coroutines.delay(100)
            }
        }
        
        rootNode?.let { root ->
            try {
                val scrollInfo = findScrollableNodeAndGetInfo(root)
                val metrics = getScreenMetrics()
                
                RawScreenData(
                    rootNode = root,
                    pixelsAbove = scrollInfo.first,
                    pixelsBelow = scrollInfo.second,
                    screenWidth = metrics.first,
                    screenHeight = metrics.second
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error getting screen analysis data", e)
                root.recycle()
                null
            }
        }
    }
    
    private fun findScrollableNodeAndGetInfo(rootNode: AccessibilityNodeInfo): Pair<Int, Int> {
        var maxScrollable: AccessibilityNodeInfo? = null
        var maxScrollableArea = 0
        var pixelsAbove = 0
        var pixelsBelow = 0
        
        fun findScrollable(node: AccessibilityNodeInfo) {
            if (node.isScrollable) {
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)
                val area = bounds.width() * bounds.height()
                if (area > maxScrollableArea) {
                    maxScrollableArea = area
                    maxScrollable = node
                }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    findScrollable(child)
                    child.recycle()
                }
            }
        }
        
        findScrollable(rootNode)
        
        maxScrollable?.let { scrollable ->
            val scrollBounds = android.graphics.Rect()
            scrollable.getBoundsInScreen(scrollBounds)
            
            // Try to get scroll position
            val scrollX = scrollable.scrollX
            val scrollY = scrollable.scrollY
            val scrollWidth = scrollable.scrollViewportWidth
            val scrollHeight = scrollable.scrollViewportHeight
            
            // Estimate pixels above and below based on scroll position
            pixelsAbove = scrollY
            pixelsBelow = maxOf(0, scrollable.childCount * 100 - scrollY - scrollHeight) // Rough estimate
            
            scrollable.recycle()
        }
        
        return Pair(pixelsAbove, pixelsBelow)
    }
    
    private fun getScreenMetrics(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }
    
    // ==================== Gesture Methods ====================
    
    /**
     * Perform a tap gesture at specified coordinates
     */
    fun clickOnPoint(x: Float, y: Float): Boolean {
        if (showDebugTap) {
            showDebugTap(x, y)
        }
        return performClick(x, y)
    }
    
    /**
     * Perform a long press gesture at specified coordinates
     */
    fun longClickOnPoint(x: Float, y: Float): Boolean {
        if (showDebugTap) {
            showDebugTap(x, y)
        }
        return performLongClick(x, y)
    }
    
    /**
     * Perform a swipe gesture between two points
     */
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
        return performSwipe(x1, y1, x2, y2, duration)
    }
    
    /**
     * Scroll down precisely using physics-based scrolling
     */
    fun scrollDownPrecisely(pixels: Int, pixelsPerSecond: Int = 1000): Boolean {
        val metrics = getScreenMetrics()
        val startY = metrics.second - 100
        val endY = startY - pixels
        val duration = (pixels * 1000L / pixelsPerSecond)
        
        return swipe(100f, startY.toFloat(), 100f, endY.toFloat(), duration)
    }
    
    /**
     * Scroll up precisely
     */
    fun scrollUpPrecisely(pixels: Int, pixelsPerSecond: Int = 1000): Boolean {
        val metrics = getScreenMetrics()
        val startY = 100
        val endY = startY + pixels
        val duration = (pixels * 1000L / pixelsPerSecond)
        
        return swipe(100f, startY.toFloat(), 100f, endY.toFloat(), duration)
    }
    
    private fun performClick(x: Float, y: Float): Boolean {
        val path = android.view.Path().apply {
            moveTo(x, y)
        }
        
        val builder = android.view.accessibility.AccessibilityGestureDescription.Builder(1)
        builder.addStroke(0, 500, path)
        
        return dispatchGesture(builder.build(), null, null)
    }
    
    private fun performLongClick(x: Float, y: Float): Boolean {
        val path = android.view.Path().apply {
            moveTo(x, y)
        }
        
        val builder = android.view.accessibility.AccessibilityGestureDescription.Builder(1)
        builder.addStroke(0, 1500, path) // Longer duration for long press
        
        return dispatchGesture(builder.build(), null, null)
    }
    
    private fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): Boolean {
        val path = android.view.Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        
        val builder = android.view.accessibility.AccessibilityGestureDescription.Builder(1)
        builder.addStroke(0, duration, path)
        
        return dispatchGesture(builder.build(), null, null)
    }
    
    // ==================== Text Input Methods ====================
    
    /**
     * Type text into the focused input field
     */
    fun typeTextInFocusedField(textToType: String): Boolean {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUSABLE)
        
        if (focusedNode != null && focusedNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            return result
        }
        
        focusedNode?.recycle()
        
        // Fallback: try to find any editable field
        val editableNode = findEditableNode(rootInActiveWindow)
        return if (editableNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            val result = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            editableNode.recycle()
            result
        } else {
            false
        }
    }
    
    private fun findEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findEditableNode(child)
                child.recycle()
                if (result != null) return result
            }
        }
        
        return null
    }
    
    /**
     * Check if typing is available (keyboard is open or editable field is focused)
     */
    fun isTypingAvailable(): Boolean {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUSABLE)
        val isEditable = focusedNode?.isEditable ?: false
        focusedNode?.recycle()
        return isEditable || findEditableNode(rootInActiveWindow) != null
    }
    
    /**
     * Press enter key on focused input
     */
    fun pressEnter(): Boolean {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUSABLE)
        
        if (focusedNode != null) {
            // Try IME enter action first
            val enterAction = focusedNode.getAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER)
            if (enterAction != null) {
                val result = focusedNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
                focusedNode.recycle()
                if (result) return true
            }
            
            // Fallback to clicking enter button
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            focusedNode.recycle()
            return true
        }
        
        return false
    }
    
    // ==================== Global Actions ====================
    
    /**
     * Perform global back action
     */
    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    /**
     * Perform global home action
     */
    fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    /**
     * Perform recents/app switcher action
     */
    fun performRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    /**
     * Expand notifications panel
     */
    fun expandNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }
    
    /**
     * Open power menu
     */
    fun openPowerMenu(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }
    
    // ==================== Screenshot Methods ====================
    
    /**
     * Capture screenshot of current screen
     * Requires API level R (30) or higher
     */
    @Suppress("DEPRECATION")
    suspend fun captureScreenshot(): Bitmap? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            AppLogger.w(TAG, "Screenshot requires API 30 or higher")
            return@withContext null
        }
        
        try {
            suspendCancellableCoroutine { continuation ->
                try {
                    val callback = object : android.hardware.display.DisplayManager.DisplayListener {
                        override fun onDisplayAdded(displayId: Int) {}
                        override fun onDisplayRemoved(displayId: Int) {}
                        override fun onDisplayChanged(displayId: Int) {
                            if (displayId == android.view.Display.DEFAULT_DISPLAY) {
                                continuation.resume(true)
                            }
                        }
                    }
                    
                    val displayManager = getSystemService(DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                    displayManager.registerDisplayListener(callback, null)
                    
                    val screenshotResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        takeScreenshot(
                            android.view.Display.DEFAULT_DISPLAY,
                            mainExecutor,
                            android.hardware.display.ScreenshotCallback { hardwareBuffer, presentationTime, colorSpace ->
                                if (hardwareBuffer != null) {
                                    val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                                    continuation.resume(bitmap)
                                } else {
                                    continuation.resume(null)
                                }
                            }
                        )
                    } else {
                        takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor) { hardwareBuffer, _, colorSpace ->
                            if (hardwareBuffer != null) {
                                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                continuation.resume(bitmap)
                            } else {
                                continuation.resume(null)
                            }
                        }
                    }
                    
                    if (!screenshotResult) {
                        continuation.resume(null)
                    }
                    
                    continuation.invokeOnCancellation {
                        try {
                            displayManager.unregisterDisplayListener(callback)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Screenshot error", e)
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Screenshot failed", e)
            null
        }
    }
    
    // ==================== Element Interaction Methods ====================
    
    /**
     * Click on an accessibility node
     */
    fun clickOnNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        
        // Try to click on the node's parent
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                return result
            }
            val temp = parent
            parent = parent.parent
            temp.recycle()
        }
        
        return false
    }
    
    /**
     * Long click on an accessibility node
     */
    fun longClickOnNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isLongClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        }
        
        // Try to long click on the node's parent
        var parent = node.parent
        while (parent != null) {
            if (parent.isLongClickable) {
                val result = parent.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                parent.recycle()
                return result
            }
            val temp = parent
            parent = parent.parent
            temp.recycle()
        }
        
        return false
    }
    
    // ==================== Visual Feedback Methods ====================
    
    /**
     * Show debug tap indicator
     */
    private fun showDebugTap(tapX: Float, tapY: Float) {
        try {
            val handler = android.os.Handler(mainLooper)
            handler.post {
                try {
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT
                    )
                    
                    val view = android.view.View(this)
                    view.setBackgroundColor(0x88FF0000.toInt())
                    view.postDelayed({
                        try {
                            windowManager?.removeView(view)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }, 500)
                    
                    windowManager?.addView(view, params)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Debug tap error", e)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Debug tap error", e)
        }
    }
    
    /**
     * Show screen flash effect
     */
    fun showScreenFlash() {
        try {
            val handler = android.os.Handler(mainLooper)
            handler.post {
                try {
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT
                    )
                    
                    val view = android.view.View(this)
                    view.setBackgroundColor(0xFFFFFFFF.toInt())
                    view.alpha = 0.3f
                    view.postDelayed({
                        try {
                            windowManager?.removeView(view)
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }, 100)
                    
                    windowManager?.addView(view, params)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Screen flash error", e)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Screen flash error", e)
        }
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Get current foreground activity name
     */
    fun getCurrentActivityName(): String? = currentActivityName
    
    /**
     * Check if keyboard is available
     */
    fun isKeyboardOpened(): Boolean {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        return inputMethodManager.isAcceptingText
    }
    
    /**
     * Get package name
     */
    fun getCurrentPackageName(): String? {
        return currentActivityName
    }
}

/**
 * Data class for simplified UI element
 */
data class SimplifiedElement(
    val text: String,
    val resourceId: String,
    val className: String,
    val contentDescription: String,
    val bounds: android.graphics.Rect,
    val depth: Int,
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isEnabled: Boolean,
    val isFocused: Boolean,
    val node: AccessibilityNodeInfo? = null
)

/**
 * Data class for raw screen data
 */
data class RawScreenData(
    val rootNode: AccessibilityNodeInfo,
    val pixelsAbove: Int,
    val pixelsBelow: Int,
    val screenWidth: Int,
    val screenHeight: Int
)
