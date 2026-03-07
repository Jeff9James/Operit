package com.ai.assistance.operit.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.ui.automation.*
import kotlinx.coroutines.*

/**
 * OperitAccessibilityService
 * Provides UI automation capabilities through Android AccessibilityService API
 */
class OperitAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "OperitAccessibilityService"
        
        @Volatile
        var instance: OperitAccessibilityService? = null
        
        fun getInstance(): OperitAccessibilityService? = instance
        
        fun isConnected(): Boolean = instance != null
    }
    
    private lateinit var windowManager: WindowManager
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentActivityName: String? = null
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        updateScreenSize()
        AppLogger.d(TAG, "Accessibility service created")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        updateScreenSize()
        AppLogger.d(TAG, "Accessibility service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentActivityName = event.packageName?.toString()
                AppLogger.d(TAG, "Activity changed to: $currentActivityName")
            }
        }
    }
    
    override fun onInterrupt() {
        AppLogger.d(TAG, "Accessibility service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        AppLogger.d(TAG, "Accessibility service destroyed")
    }
    
    private fun updateScreenSize() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }
    
    fun getScreenSize(): Pair<Int, Int> = Pair(screenWidth, screenHeight)
    
    /**
     * Get UI hierarchy as XML
     */
    fun getUIHierarchyXml(): String {
        return try {
            val rootNode = rootInActiveWindow ?: return ""
            val parser = SemanticParser()
            val analysis = parser.parseNodeTree(rootNode)
            rootNode.recycle()
            analysis.uiRepresentation
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting UI hierarchy", e)
            ""
        }
    }
    
    /**
     * Simplified UI hierarchy
     */
    fun getSimplifiedUIHierarchy(): String {
        return try {
            val rootNode = rootInActiveWindow ?: return ""
            
            val sb = StringBuilder()
            dumpSimplifiedNode(rootNode, sb, 0)
            
            rootNode.recycle()
            sb.toString()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting simplified hierarchy", e)
            ""
        }
    }
    
    private fun dumpSimplifiedNode(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (!node.isVisibleToUser) return
        
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val resourceId = node.viewIdResourceName ?: ""
        
        if (text.isNotBlank() || contentDesc.isNotBlank() || node.isClickable) {
            val indent = "  ".repeat(depth)
            val className = node.className?.toString()?.substringAfterLast(".") ?: "Unknown"
            sb.appendLine("$indent- $className: $text (id: $resourceId)")
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                dumpSimplifiedNode(child, sb, depth + 1)
                child.recycle()
            }
        }
    }
    
    /**
     * Execute click at coordinates
     */
    fun performClickAt(x: Int, y: Int): Boolean {
        AppLogger.d(TAG, "Click at: ($x, $y)")
        
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }
        
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 500))
        
        return dispatchGesture(builder.build(), null, Handler(Looper.getMainLooper()))
    }
    
    /**
     * Execute long press at coordinates
     */
    fun performLongPressAt(x: Int, y: Int): Boolean {
        AppLogger.d(TAG, "Long press at: ($x, $y)")
        
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }
        
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 1500))
        
        return dispatchGesture(builder.build(), null, Handler(Looper.getMainLooper()))
    }
    
    /**
     * Execute swipe gesture
     */
    fun performSwipeGesture(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long): Boolean {
        AppLogger.d(TAG, "Swipe: ($startX, $startY) -> ($endX, $endY), duration=$duration")
        
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }
        
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        
        return dispatchGesture(builder.build(), null, Handler(Looper.getMainLooper()))
    }
    
    /**
     * Execute scroll down
     */
    fun performScrollDown(): Boolean {
        val startY = (screenHeight * 0.8).toInt()
        val endY = (screenHeight * 0.2).toInt()
        val centerX = screenWidth / 2
        
        return performSwipeGesture(centerX, startY, centerX, endY, 500)
    }
    
    /**
     * Execute scroll up
     */
    fun performScrollUp(): Boolean {
        val startY = (screenHeight * 0.2).toInt()
        val endY = (screenHeight * 0.8).toInt()
        val centerX = screenWidth / 2
        
        return performSwipeGesture(centerX, startY, centerX, endY, 500)
    }
    
    /**
     * Input text
     */
    fun performTextInput(text: String): Boolean {
        AppLogger.d(TAG, "Input text: $text")
        
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUSABLE)
        
        if (focusedNode != null && focusedNode.isEditable) {
            val arguments = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            return result
        }
        
        focusedNode?.recycle()
        return false
    }
    
    /**
     * Get current activity
     */
    fun getCurrentActivity(): String? = currentActivityName
    
    /**
     * Check if connected
     */
    fun isServiceConnected(): Boolean = instance != null
    
    /**
     * Get interactive elements
     */
    fun getInteractiveElements(): Map<Int, InteractiveElement> {
        return try {
            val rootNode = rootInActiveWindow ?: return emptyMap()
            val parser = SemanticParser()
            val analysis = parser.parseNodeTree(rootNode)
            rootNode.recycle()
            analysis.elementMap
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error getting interactive elements", e)
            emptyMap()
        }
    }
    
    /**
     * Execute back action
     */
    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    
    /**
     * Execute home action
     */
    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    
    /**
     * Execute recents action
     */
    fun performRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    
    /**
     * Execute notifications action
     */
    fun performNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    
    /**
     * Execute power dialog action
     */
    fun performPowerDialog(): Boolean = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    
    /**
     * Execute a task using AI Agent
     */
    fun executeTask(task: String, callback: (AgentResult) -> Unit) {
        serviceScope.launch {
            val agent = Agent(this@OperitAccessibilityService)
            val result = agent.run(task)
            callback(result)
        }
    }
}
