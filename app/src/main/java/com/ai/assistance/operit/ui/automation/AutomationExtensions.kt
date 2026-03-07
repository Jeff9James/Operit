package com.ai.assistance.operit.ui.automation

import android.content.Context
import com.ai.assistance.operit.services.OperitAccessibilityService

/**
 * UI Automation Utilities
 * Provides easy access to all UI automation components
 */
object AutomationExtensions {
    
    /**
     * Get the OperitAccessibilityService instance
     */
    fun getAccessibilityService(): OperitAccessibilityService? {
        return OperitAccessibilityService.getInstance()
    }
    
    /**
     * Check if UI automation is available
     */
    fun isAutomationAvailable(): Boolean {
        return OperitAccessibilityService.getInstance() != null
    }
    
    /**
     * Get Perception instance
     */
    fun getPerception(): Perception? {
        val service = OperitAccessibilityService.getInstance() ?: return null
        return Perception(service)
    }
    
    /**
     * Get Eyes instance
     */
    fun getEyes(): Eyes? {
        val service = OperitAccessibilityService.getInstance() ?: return null
        return Eyes(service)
    }
    
    /**
     * Get Finger instance
     */
    fun getFinger(): Finger? {
        val service = OperitAccessibilityService.getInstance() ?: return null
        return Finger(service)
    }
    
    /**
     * Get Agent instance
     */
    fun getAgent(): Agent? {
        val service = OperitAccessibilityService.getInstance() ?: return null
        return Agent(service)
    }
    
    /**
     * Get ActionExecutor instance
     */
    fun getActionExecutor(): ActionExecutor? {
        val service = OperitAccessibilityService.getInstance() ?: return null
        return ActionExecutor(service)
    }
}

/**
 * Extension function to get Perception instance
 */
fun Context.perception(): Perception? {
    val service = OperitAccessibilityService.getInstance() ?: return null
    return Perception(service)
}

/**
 * Extension function to get Eyes instance
 */
fun Context.eyes(): Eyes? {
    val service = OperitAccessibilityService.getInstance() ?: return null
    return Eyes(service)
}

/**
 * Extension function to get Finger instance
 */
fun Context.finger(): Finger? {
    val service = OperitAccessibilityService.getInstance() ?: return null
    return Finger(service)
}

/**
 * Extension function to get Agent instance
 */
fun Context.agent(): Agent? {
    val service = OperitAccessibilityService.getInstance() ?: return null
    return Agent(service)
}

/**
 * Extension function to get ActionExecutor instance
 */
fun Context.actionExecutor(): ActionExecutor? {
    val service = OperitAccessibilityService.getInstance() ?: return null
    return ActionExecutor(service)
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
