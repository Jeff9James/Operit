package com.ai.assistance.operit.ui.automation

/**
 * Memory Manager - Agent Memory and Prompt Construction
 * Maintains conversation history and constructs prompts for the LLM
 * Based on Blurr's MemoryManager implementation
 */
class MemoryManager {
    
    private val history = mutableListOf<HistoryEntry>()
    private var currentTask: String = ""
    private var memory: String = ""
    private var readState: String = ""
    
    /**
     * Update memory with result of previous step
     */
    fun updateHistory(
        step: Int,
        action: AgentAction,
        result: ActionResult,
        evaluation: String
    ) {
        history.add(
            HistoryEntry(
                step = step,
                action = action.toString(),
                result = result.message,
                evaluation = evaluation
            )
        )
    }
    
    /**
     * Set current task
     */
    fun setTask(task: String) {
        currentTask = task
    }
    
    /**
     * Update agent's memory
     */
    fun updateMemory(newMemory: String) {
        memory = newMemory
    }
    
    /**
     * Update read state (for file operations)
     */
    fun updateReadState(readContent: String) {
        readState = readContent
    }
    
    /**
     * Build history description for the prompt
     */
    fun buildHistoryDescription(): String {
        if (history.isEmpty()) {
            return "No previous steps."
        }
        
        val sb = StringBuilder()
        sb.appendLine("## History of Steps")
        
        for (entry in history) {
            sb.appendLine()
            sb.appendLine("### Step ${entry.step}")
            sb.appendLine("- Action: ${entry.action}")
            sb.appendLine("- Result: ${entry.result}")
            sb.appendLine("- Evaluation: ${entry.evaluation}")
        }
        
        return sb.toString()
    }
    
    /**
     * Build state description for the prompt
     */
    fun buildStateDescription(): String {
        val sb = StringBuilder()
        
        if (memory.isNotBlank()) {
            sb.appendLine("### Agent Memory")
            sb.appendLine(memory)
            sb.appendLine()
        }
        
        if (readState.isNotBlank()) {
            sb.appendLine("### Read Content")
            sb.appendLine(readState)
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    /**
     * Build complete prompt with system message, task, screen state, and history
     */
    fun buildPrompt(
        systemPrompt: String,
        screenAnalysis: ScreenAnalysis,
        availableActions: List<String>
    ): String {
        val sb = StringBuilder()
        
        // System prompt
        sb.appendLine(systemPrompt)
        sb.appendLine()
        
        // Available actions
        sb.appendLine("## Available Actions")
        for (action in availableActions) {
            sb.appendLine("- $action")
        }
        sb.appendLine()
        
        // Current task
        sb.appendLine("## Current Task")
        sb.appendLine(currentTask)
        sb.appendLine()
        
        // Screen state
        sb.appendLine("## Current Screen State")
        sb.appendLine("### Activity")
        sb.appendLine(screenAnalysis.activityName)
        sb.appendLine()
        
        sb.appendLine("### UI Elements")
        sb.appendLine(screenAnalysis.uiRepresentation)
        sb.appendLine()
        
        sb.appendLine("### Scroll Info")
        sb.appendLine("Pixels above: ${screenAnalysis.pixelsAbove}")
        sb.appendLine("Pixels below: ${screenAnalysis.pixelsBelow}")
        sb.appendLine()
        
        sb.appendLine("### Keyboard")
        sb.appendLine("Open: ${screenAnalysis.keyboardOpen}")
        sb.appendLine()
        
        // History
        sb.append(buildHistoryDescription())
        sb.appendLine()
        
        // State
        sb.append(buildStateDescription())
        
        return sb.toString()
    }
    
    /**
     * Clear all history and state
     */
    fun clear() {
        history.clear()
        currentTask = ""
        memory = ""
        readState = ""
    }
    
    /**
     * Get history size
     */
    fun getHistorySize(): Int = history.size
}

/**
 * History entry for tracking agent steps
 */
data class HistoryEntry(
    val step: Int,
    val action: String,
    val result: String,
    val evaluation: String
)
