package com.ai.assistance.operit.ui.automation

import android.content.Context

/**
 * UI Automation Utilities
 * Provides easy access to all UI automation components
 */
object AutomationExtensions {
    
    /**
     * Get the ScreenInteractionService instance
     */
    fun getScreenInteractionService(): ScreenInteractionService? {
        return ScreenInteractionService.instance
    }
    
    /**
     * Check if UI automation is available
     */
    fun isAutomationAvailable(): Boolean {
        return ScreenInteractionService.instance != null
    }
    
    /**
     * Get the accessibility service instance
     */
    fun getAccessibilityService(): com.ai.assistance.operit.services.OperitAccessibilityService? {
        return com.ai.assistance.operit.services.OperitAccessibilityService.getInstance()
    }
}

/**
 * Extension function to get Perception instance
 */
fun Context.perception(): Perception {
    return Perception(this)
}

/**
 * Extension function to get Eyes instance
 */
fun Context.eyes(): Eyes {
    return Eyes(this)
}

/**
 * Extension function to get Finger instance
 */
fun Context.finger(): Finger {
    return Finger(this)
}

/**
 * Extension function to get Agent instance
 */
fun Context.agent(): Agent {
    return Agent(this)
}

/**
 * Extension function to get ActionExecutor instance
 */
fun Context.actionExecutor(): ActionExecutor {
    return ActionExecutor(this)
}

/**
 * Extension function to get SemanticParser instance
 */
fun Context.semanticParser(): SemanticParser {
    return SemanticParser()
}

/**
 * Extension function to get MemoryManager instance
 */
fun Context.memoryManager(): MemoryManager {
    return MemoryManager()
}
