package com.ai.assistance.operit.ui.automation

import com.ai.assistance.operit.services.OperitAccessibilityService
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.*

/**
 * AgentResult
 * Result of running the AI agent
 */
sealed class AgentResult {
    data class Success(val message: String, val actions: List<String> = emptyList()) : AgentResult()
    data class Error(val message: String, val error: Throwable? = null) : AgentResult()
    object Timeout : AgentResult()
    object ServiceNotConnected : AgentResult()
}

/**
 * Agent
 * SENSE-THINK-ACT loop implementation
 * Orchestrates perception, reasoning, and action execution
 */
class Agent(private val service: OperitAccessibilityService) {
    
    companion object {
        private const val TAG = "Agent"
        private const val MAX_ITERATIONS = 10
        private const val TIMEOUT_MS = 60000L
    }
    
    private val perception = Perception(service)
    private val finger = Finger(service)
    private val actionExecutor = ActionExecutor(service)
    private val agentScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Run the agent with a task
     */
    suspend fun run(task: String): AgentResult = withContext(Dispatchers.Default) {
        if (!service.isServiceConnected()) {
            AppLogger.e(TAG, "Service not connected")
            return@withContext AgentResult.ServiceNotConnected
        }
        
        try {
            AppLogger.d(TAG, "Starting task: $task")
            
            // SENSE: Get screen state
            val screenState = perception.analyzeScreenSimplified()
            AppLogger.d(TAG, "Screen state: $screenState")
            
            // For now, we'll use a simple action execution based on task parsing
            // In a full implementation, this would integrate with an LLM
            val actions = parseTaskToActions(task)
            
            var success = true
            val executedActions = mutableListOf<String>()
            
            for (action in actions) {
                val result = actionExecutor.execute(action)
                if (result) {
                    executedActions.add(action)
                    delay(500) // Small delay between actions
                } else {
                    AppLogger.w(TAG, "Failed action: $action")
                    success = false
                }
            }
            
            if (executedActions.isNotEmpty()) {
                AgentResult.Success(
                    message = "Executed ${executedActions.size} actions",
                    actions = executedActions
                )
            } else {
                AgentResult.Error(message = "No actions could be executed")
            }
            
        } catch (e: TimeoutCancellationException) {
            AppLogger.e(TAG, "Task timeout", e)
            AgentResult.Timeout
        } catch (e: Exception) {
            AppLogger.e(TAG, "Task error", e)
            AgentResult.Error(message = e.message ?: "Unknown error", error = e)
        }
    }
    
    /**
     * Simple task parsing to actions
     * In a full implementation, this would use LLM-based reasoning
     */
    private fun parseTaskToActions(task: String): List<String> {
        val lowerTask = task.lowercase()
        val actions = mutableListOf<String>()
        
        when {
            lowerTask.contains("click") || lowerTask.contains("tap") -> {
                // Extract coordinates or element info if present
                // For now, assume it's a simple click
                actions.add("tap:center")
            }
            lowerTask.contains("scroll down") || lowerTask.contains("swipe down") -> {
                actions.add("scrollDown")
            }
            lowerTask.contains("scroll up") || lowerTask.contains("swipe up") -> {
                actions.add("scrollUp")
            }
            lowerTask.contains("back") -> {
                actions.add("back")
            }
            lowerTask.contains("home") -> {
                actions.add("home")
            }
            lowerTask.contains("type") || lowerTask.contains("input") || lowerTask.contains("enter") -> {
                // Extract text to input
                val textMatch = Regex("""(?:type|input|enter)\s+(.+)""").find(lowerTask)
                if (textMatch != null) {
                    val text = textMatch.groupValues[1]
                    actions.add("input:$text")
                }
            }
            else -> {
                // Try to find element by text and click
                actions.add("click:$task")
            }
        }
        
        return actions
    }
    
    /**
     * Cleanup
     */
    fun destroy() {
        agentScope.cancel()
    }
}
