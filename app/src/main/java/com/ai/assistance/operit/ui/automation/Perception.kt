package com.ai.assistance.operit.ui.automation

import com.ai.assistance.operit.services.OperitAccessibilityService

/**
 * Perception - Screen analysis module combining Eyes and SemanticParser
 * Provides high-level screen understanding capabilities
 */
class Perception(private val service: OperitAccessibilityService) {
    
    private val eyes = Eyes(service)
    private val finger = Finger(service)
    private val parser = SemanticParser()
    
    companion object {
        /**
         * Get singleton instance
         */
        fun getInstance(): Perception? {
            val service = OperitAccessibilityService.getInstance()
            return service?.let { Perception(it) }
        }
    }
    
    /**
     * Get current screen analysis
     */
    fun analyzeScreen(): UIAnalysis {
        val hierarchy = eyes.getUIHierarchy()
        val elements = eyes.getInteractiveElements()
        
        return UIAnalysis(elements, hierarchy)
    }
    
    /**
     * Get simplified screen analysis
     */
    fun analyzeScreenSimplified(): String {
        return eyes.getSimplifiedHierarchy()
    }
    
    /**
     * Find element by text
     */
    fun findByText(text: String): InteractiveElement? {
        val analysis = analyzeScreen()
        return parser.findElementByText(analysis, text)
    }
    
    /**
     * Find element by resource ID
     */
    fun findByResourceId(resourceId: String): InteractiveElement? {
        val analysis = analyzeScreen()
        return parser.findElementById(analysis, resourceId)
    }
    
    /**
     * Find clickable elements
     */
    fun findClickableElements(): List<InteractiveElement> {
        val analysis = analyzeScreen()
        return parser.findClickableElements(analysis)
    }
    
    /**
     * Find scrollable elements
     */
    fun findScrollableElements(): List<InteractiveElement> {
        val analysis = analyzeScreen()
        return parser.findScrollableElements(analysis)
    }
    
    /**
     * Get current activity
     */
    fun getCurrentActivity(): String? {
        return eyes.getCurrentActivity()
    }
    
    /**
     * Get screen size
     */
    fun getScreenSize(): Pair<Int, Int> {
        return eyes.getScreenSize()
    }
    
    /**
     * Check if service is connected
     */
    fun isConnected(): Boolean {
        return eyes.isConnected()
    }
}
