package com.ai.assistance.operit.ui.automation

import android.content.Context
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * AI Agent - SENSE-THINK-ACT Loop Orchestrator
 * Executes automation tasks using the Perception-Action cycle
 * Based on Blurr's Agent implementation
 */
class Agent(private val context: Context) {
    
    companion object {
        private const val TAG = "Agent"
        private const val MAX_STEPS = 20
    }
    
    private val perception: Perception = Perception(context)
    private val actionExecutor: ActionExecutor = ActionExecutor(context)
    private val memoryManager: MemoryManager = MemoryManager()
    
    private var currentTask: String = ""
    private var isRunning = false
    private var elementMap: Map<Int, InteractiveElement> = emptyMap()
    
    // Task queue for background execution
    private val taskQueue = ConcurrentLinkedQueue<String>()
    
    /**
     * Execute a task with the SENSE-THINK-ACT loop
     */
    suspend fun run(task: String): AgentResult = withContext(Dispatchers.IO) {
        currentTask = task
        memoryManager.setTask(task)
        isRunning = true
        
        AppLogger.d(TAG, "Starting task: $task")
        
        var step = 0
        var lastScreenSignature = ""
        
        while (isRunning && step < MAX_STEPS) {
            step++
            AppLogger.d(TAG, "Step $step of $MAX_STEPS")
            
            // SENSE: Observe current screen
            val screenAnalysis = perception.analyze()
            elementMap = screenAnalysis.elementMap
            actionExecutor.setElementMap(elementMap)
            
            // Check for screen change
            val currentSignature = perception.getXmlHierarchy()
            val screenChanged = currentSignature != lastScreenSignature
            lastScreenSignature = currentSignature
            
            // Build prompt for LLM
            val prompt = memoryManager.buildPrompt(
                systemPrompt = getSystemPrompt(),
                screenAnalysis = screenAnalysis,
                availableActions = getAvailableActions()
            )
            
            // THINK: Get decision from LLM (simplified - would integrate with actual LLM)
            val agentOutput = getLLMResponse(prompt)
            
            // Update memory
            if (agentOutput.memory.isNotBlank()) {
                memoryManager.updateMemory(agentOutput.memory)
            }
            
            // ACT: Execute actions
            var actionFailed = false
            for (action in agentOutput.actions) {
                // Check if task is done
                if (action is Done) {
                    isRunning = false
                    AppLogger.d(TAG, "Task completed: ${action.text}")
                    return@withContext AgentResult(
                        success = action.success,
                        message = action.text,
                        steps = step
                    )
                }
                
                val result = actionExecutor.execute(action)
                
                // Evaluate action result
                val evaluation = if (result.success) {
                    "Action succeeded: ${result.message}"
                } else {
                    "Action failed: ${result.message}"
                }
                
                // Update history
                memoryManager.updateHistory(step, action, result, evaluation)
                
                // Check for file read
                if (action is ReadFile && result.extractedContent != null) {
                    memoryManager.updateReadState(result.extractedContent)
                }
                
                // If action failed and we need screen change, break
                if (!result.success && !screenChanged) {
                    // Wait for screen to update
                    kotlinx.coroutines.delay(1000)
                }
                
                // If action failed significantly, stop
                if (!result.success && action !is Wait) {
                    actionFailed = true
                    break
                }
                
                // Small delay between actions
                kotlinx.coroutines.delay(300)
            }
            
            // If we've reached max steps or failed too many times
            if (step >= MAX_STEPS) {
                AppLogger.w(TAG, "Max steps reached")
                return@withContext AgentResult(
                    success = false,
                    message = "Task not completed: reached maximum steps",
                    steps = step
                )
            }
        }
        
        AgentResult(
            success = false,
            message = "Task not completed: stopped unexpectedly",
            steps = step
        )
    }
    
    /**
     * Stop the agent
     */
    fun stop() {
        isRunning = false
        memoryManager.clear()
    }
    
    /**
     * Get system prompt for the agent
     */
    private fun getSystemPrompt(): String {
        return """
You are an AI assistant that can interact with Android apps using UI automation.

## Your Capabilities
- You can see the current screen's UI elements and their properties
- You can tap, long press, scroll, and input text
- You can navigate between apps, go back, and go home
- You can read and write files

## Available Actions
- TapElement(id): Tap on an element by its index
- LongPressElement(id): Long press on an element
- InputText(text): Type text into focused field
- TapElementInputTextPressEnter(id, text): Tap element, type, and press enter
- Speak(message): Speak a message to the user
- Ask(question): Ask the user a question
- OpenApp(appName): Open an app by name
- Back: Navigate back
- Home: Go to home screen
- SwitchApp: Open app switcher
- Wait(seconds): Wait for specified seconds
- ScrollDown(pixels): Scroll down
- ScrollUp(pixels): Scroll up
- SearchGoogle(query): Search on Google
- Done(success, text): Complete the task

## UI Element Format
Elements are shown with:
- Index: Unique ID for referencing
- Text: The text displayed
- Resource ID: Android resource ID
- Class: View class name
- Content Description: Accessibility description
- Bounds: Screen coordinates [left,top][right,bottom]
- States: clickable, scrollable, editable, etc.

## Guidelines
1. Analyze the screen and identify the best action
2. Use element indexes to interact with specific UI elements
3. After each action, evaluate if it succeeded
4. If stuck, try scrolling or going back
5. When task is complete, use Done action with success=true
6. If task cannot be completed, use Done with success=false and explain why
        """.trimIndent()
    }
    
    /**
     * Get list of available action names
     */
    private fun getAvailableActions(): List<String> {
        return listOf(
            "TapElement(id)",
            "LongPressElement(id)",
            "InputText(text)",
            "TapElementInputTextPressEnter(id, text)",
            "Speak(message)",
            "Ask(question)",
            "OpenApp(appName)",
            "Back",
            "Home",
            "SwitchApp",
            "Wait(seconds)",
            "ScrollDown(pixels)",
            "ScrollUp(pixels)",
            "SearchGoogle(query)",
            "Done(success, text)"
        )
    }
    
    /**
     * Get LLM response - Simplified version
     * In production, this would integrate with Gemini or other LLM
     */
    private suspend fun getLLMResponse(prompt: String): AgentOutput {
        // This is a placeholder - in production, would call actual LLM
        // For now, we'll return a simple response that asks user for input
        
        AppLogger.d(TAG, "Getting LLM response...")
        
        // Placeholder - would call Gemini API here
        return AgentOutput(
            thinking = "Analyzing screen...",
            evaluationPreviousGoal = "",
            memory = "",
            nextGoal = "Ask user for guidance",
            actions = listOf(Ask("What would you like me to do next?"))
        )
    }
    
    /**
     * Parse LLM JSON response into AgentOutput
     */
    fun parseLLMResponse(jsonString: String): AgentOutput {
        return try {
            val json = JSONObject(jsonString)
            
            val actions = mutableListOf<AgentAction>()
            val actionsArray = json.optJSONArray("actions")
            
            if (actionsArray != null) {
                for (i in 0 until actionsArray.length()) {
                    val actionObj = actionsArray.getJSONObject(i)
                    val actionType = actionObj.getString("type")
                    val actionData = actionObj.optJSONObject("data")
                    
                    val action = when (actionType) {
                        "TapElement" -> TapElement(actionData?.getInt("elementId") ?: 0)
                        "LongPressElement" -> LongPressElement(actionData?.getInt("elementId") ?: 0)
                        "InputText" -> InputText(actionData?.getString("text") ?: "")
                        "TapElementInputTextPressEnter" -> TapElementInputTextPressEnter(
                            actionData?.getInt("elementId") ?: 0,
                            actionData?.getString("text") ?: ""
                        )
                        "Speak" -> Speak(actionData?.getString("message") ?: "")
                        "Ask" -> Ask(actionData?.getString("question") ?: "")
                        "OpenApp" -> OpenApp(actionData?.getString("appName") ?: "")
                        "Back" -> Back
                        "Home" -> Home
                        "SwitchApp" -> SwitchApp
                        "Wait" -> Wait(actionData?.getInt("durationSeconds") ?: 5)
                        "ScrollDown" -> ScrollDown(actionData?.getInt("amount") ?: 500)
                        "ScrollUp" -> ScrollUp(actionData?.getInt("amount") ?: 500)
                        "SearchGoogle" -> SearchGoogle(actionData?.getString("query") ?: "")
                        "Done" -> Done(
                            actionData?.getBoolean("success") ?: false,
                            actionData?.getString("text") ?: ""
                        )
                        else -> null
                    }
                    
                    action?.let { actions.add(it) }
                }
            }
            
            AgentOutput(
                thinking = json.optString("thinking", ""),
                evaluationPreviousGoal = json.optString("evaluationPreviousGoal", ""),
                memory = json.optString("memory", ""),
                nextGoal = json.optString("nextGoal", ""),
                actions = actions
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error parsing LLM response", e)
            AgentOutput(
                thinking = "Error parsing response",
                actions = listOf(Ask("I encountered an error. What would you like me to do?"))
            )
        }
    }
    
    /**
     * Add task to queue
     */
    fun addTaskToQueue(task: String) {
        taskQueue.offer(task)
    }
    
    /**
     * Process next task in queue
     */
    suspend fun processNextTask(): AgentResult? {
        val task = taskQueue.poll() ?: return null
        return run(task)
    }
    
    /**
     * Get queue size
     */
    fun getQueueSize(): Int = taskQueue.size
}

/**
 * Result of agent task execution
 */
data class AgentResult(
    val success: Boolean,
    val message: String,
    val steps: Int
)
