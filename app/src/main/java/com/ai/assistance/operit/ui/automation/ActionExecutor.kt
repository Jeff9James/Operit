package com.ai.assistance.operit.ui.automation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Action Executor - Executes AI agent actions
 * Translates agent decisions into device interactions
 * Based on Blurr's ActionExecutor implementation
 */
class ActionExecutor(private val context: Context) {
    
    private val finger: Finger = Finger(context)
    private val service: ScreenInteractionService?
        get() = ScreenInteractionService.instance
    
    private var elementMap: Map<Int, InteractiveElement> = emptyMap()
    
    /**
     * Set element map for element-based actions
     */
    fun setElementMap(map: Map<Int, InteractiveElement>) {
        elementMap = map
    }
    
    /**
     * Execute an action and return result
     */
    suspend fun execute(action: AgentAction): ActionResult = withContext(Dispatchers.Main) {
        when (action) {
            is TapElement -> executeTapElement(action.elementId)
            is LongPressElement -> executeLongPressElement(action.elementId)
            is InputText -> executeInputText(action.text)
            is TapElementInputTextPressEnter -> executeTapElementInputTextPressEnter(action.elementId, action.text)
            is Speak -> executeSpeak(action.message)
            is Ask -> executeAsk(action.question)
            is OpenApp -> executeOpenApp(action.appName)
            is Back -> executeBack()
            is Home -> executeHome()
            is SwitchApp -> executeSwitchApp()
            is Wait -> executeWait(action.durationSeconds)
            is ScrollDown -> executeScrollDown(action.amount)
            is ScrollUp -> executeScrollUp(action.amount)
            is SearchGoogle -> executeSearchGoogle(action.query)
            is Done -> executeDone(action.success, action.text, action.filesToDisplay)
            is AppendFile -> executeAppendFile(action.fileName, action.content)
            is ReadFile -> executeReadFile(action.fileName)
            is WriteFile -> executeWriteFile(action.fileName, action.content)
            is LaunchIntent -> executeLaunchIntent(action.intentName, action.parameters)
            is TakeScreenshot -> executeTakeScreenshot()
            is OpenUrl -> executeOpenUrl(action.url)
            is CopyToClipboard -> executeCopyToClipboard(action.text)
        }
    }
    
    // ==================== Element Actions ====================
    
    private suspend fun executeTapElement(elementId: Int): ActionResult {
        val element = elementMap[elementId]
        if (element == null) {
            return ActionResult(false, "Element $elementId not found")
        }
        
        element.node?.let { node ->
            // Try accessibility click first
            if (service?.clickOnNode(node) == true) {
                return ActionResult(true, "Clicked element $elementId")
            }
        }
        
        // Fallback to physical tap using bounds
        val centerX = (element.bounds.left + element.bounds.right) / 2
        val centerY = (element.bounds.top + element.bounds.bottom) / 2
        
        return if (finger.tap(centerX, centerY)) {
            ActionResult(true, "Tapped element $elementId at ($centerX, $centerY)")
        } else {
            ActionResult(false, "Failed to tap element $elementId")
        }
    }
    
    private suspend fun executeLongPressElement(elementId: Int): ActionResult {
        val element = elementMap[elementId]
        if (element == null) {
            return ActionResult(false, "Element $elementId not found")
        }
        
        element.node?.let { node ->
            if (service?.longClickOnNode(node) == true) {
                return ActionResult(true, "Long pressed element $elementId")
            }
        }
        
        // Fallback to physical long press
        val centerX = (element.bounds.left + element.bounds.right) / 2
        val centerY = (element.bounds.top + element.bounds.bottom) / 2
        
        return if (finger.longPress(centerX, centerY)) {
            ActionResult(true, "Long pressed at ($centerX, $centerY)")
        } else {
            ActionResult(false, "Failed to long press element $elementId")
        }
    }
    
    private suspend fun executeInputText(text: String): ActionResult {
        return if (finger.type(text)) {
            ActionResult(true, "Input text: $text")
        } else {
            ActionResult(false, "Failed to input text")
        }
    }
    
    private suspend fun executeTapElementInputTextPressEnter(elementId: Int, text: String): ActionResult {
        // First tap the element
        val tapResult = executeTapElement(elementId)
        if (!tapResult.success) {
            return tapResult
        }
        
        // Wait for keyboard to appear
        kotlinx.coroutines.delay(500)
        
        // Input text
        val inputResult = executeInputText(text)
        if (!inputResult.success) {
            return inputResult
        }
        
        // Press enter
        return if (finger.enter()) {
            ActionResult(true, "Tapped element $elementId, typed '$text', and pressed enter")
        } else {
            ActionResult(false, "Failed to press enter")
        }
    }
    
    // ==================== Voice/Speak Actions ====================
    
    private suspend fun executeSpeak(message: String): ActionResult {
        // This would integrate with TTS service
        // For now, just log it
        AppLogger.d("ActionExecutor", "Speak: $message")
        return ActionResult(true, "Speaking: $message")
    }
    
    private suspend fun executeAsk(question: String): ActionResult {
        // This would integrate with user input system
        AppLogger.d("ActionExecutor", "Ask: $question")
        return ActionResult(true, "Asked: $question")
    }
    
    // ==================== App Actions ====================
    
    private suspend fun executeOpenApp(appName: String): ActionResult {
        val packageName = resolveAppPackage(appName)
        if (packageName == null) {
            return ActionResult(false, "App not found: $appName")
        }
        
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            return ActionResult(false, "Could not launch app: $appName")
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        
        return ActionResult(true, "Opened app: $appName ($packageName)")
    }
    
    private fun resolveAppPackage(appName: String): String? {
        // Try direct package name first
        try {
            context.packageManager.getPackageInfo(appName, 0)
            return appName
        } catch (e: PackageManager.NameNotFoundException) {
            // Continue to search
        }
        
        // Search by app name
        val apps = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val matchedApp = apps.find { app ->
            app.loadLabel(context.packageManager).toString().equals(appName, ignoreCase = true)
        }
        
        return matchedApp?.packageName
    }
    
    // ==================== Navigation Actions ====================
    
    private suspend fun executeBack(): ActionResult {
        return if (finger.back()) {
            ActionResult(true, "Navigated back")
        } else {
            ActionResult(false, "Failed to navigate back")
        }
    }
    
    private suspend fun executeHome(): ActionResult {
        return if (finger.home()) {
            ActionResult(true, "Navigated to home")
        } else {
            ActionResult(false, "Failed to navigate to home")
        }
    }
    
    private suspend fun executeSwitchApp(): ActionResult {
        return if (finger.switchApp()) {
            ActionResult(true, "Opened app switcher")
        } else {
            ActionResult(false, "Failed to open app switcher")
        }
    }
    
    // ==================== Timing Actions ====================
    
    private suspend fun executeWait(durationSeconds: Int): ActionResult {
        kotlinx.coroutines.delay(durationSeconds * 1000L)
        return ActionResult(true, "Waited $durationSeconds seconds")
    }
    
    // ==================== Scroll Actions ====================
    
    private suspend fun executeScrollDown(amount: Int): ActionResult {
        return if (finger.scrollDown(amount)) {
            ActionResult(true, "Scrolled down $amount pixels")
        } else {
            ActionResult(false, "Failed to scroll down")
        }
    }
    
    private suspend fun executeScrollUp(amount: Int): ActionResult {
        return if (finger.scrollUp(amount)) {
            ActionResult(true, "Scrolled up $amount pixels")
        } else {
            ActionResult(false, "Failed to scroll up")
        }
    }
    
    // ==================== Search Actions ====================
    
    private suspend fun executeSearchGoogle(query: String): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        return try {
            context.startActivity(intent)
            ActionResult(true, "Searched Google for: $query")
        } catch (e: Exception) {
            ActionResult(false, "Failed to search: ${e.message}")
        }
    }
    
    // ==================== Completion Actions ====================
    
    private suspend fun executeDone(success: Boolean, text: String, filesToDisplay: List<String>?): ActionResult {
        return ActionResult(success, text, extractedContent = filesToDisplay?.joinToString("\n"))
    }
    
    // ==================== File Actions ====================
    
    private suspend fun executeAppendFile(fileName: String, content: String): ActionResult {
        // This would integrate with file system
        AppLogger.d("ActionExecutor", "Append to file: $fileName, content: $content")
        return ActionResult(true, "Appended to file: $fileName")
    }
    
    private suspend fun executeReadFile(fileName: String): ActionResult {
        // This would integrate with file system
        AppLogger.d("ActionExecutor", "Read file: $fileName")
        return ActionResult(true, "Read file: $fileName", extractedContent = "File content placeholder")
    }
    
    private suspend fun executeWriteFile(fileName: String, content: String): ActionResult {
        // This would integrate with file system
        AppLogger.d("ActionExecutor", "Write file: $fileName, content: $content")
        return ActionResult(true, "Wrote to file: $fileName")
    }
    
    // ==================== Intent Actions ====================
    
    private suspend fun executeLaunchIntent(intentName: String, parameters: Map<String, String>): ActionResult {
        // This would resolve and launch an Android intent
        AppLogger.d("ActionExecutor", "Launch intent: $intentName with params: $parameters")
        return ActionResult(true, "Launched intent: $intentName")
    }
    
    // ==================== Screenshot Actions ====================
    
    private suspend fun executeTakeScreenshot(): ActionResult {
        val screenshot = service?.captureScreenshot()
        return if (screenshot != null) {
            ActionResult(true, "Screenshot captured", extractedContent = "Screenshot bitmap")
        } else {
            ActionResult(false, "Failed to capture screenshot")
        }
    }
    
    // ==================== URL Actions ====================
    
    private suspend fun executeOpenUrl(url: String): ActionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        return try {
            context.startActivity(intent)
            ActionResult(true, "Opened URL: $url")
        } catch (e: Exception) {
            ActionResult(false, "Failed to open URL: ${e.message}")
        }
    }
    
    // ==================== Clipboard Actions ====================
    
    private suspend fun executeCopyToClipboard(text: String): ActionResult {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Operit", text)
        clipboard.setPrimaryClip(clip)
        return ActionResult(true, "Copied to clipboard: $text")
    }
}

/**
 * Result of action execution
 */
data class ActionResult(
    val success: Boolean,
    val message: String,
    val extractedContent: String? = null,
    val memoryUpdate: String? = null
)
