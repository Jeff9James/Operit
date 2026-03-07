package com.ai.assistance.operit.ui.automation

import com.ai.assistance.operit.services.OperitAccessibilityService
import com.ai.assistance.operit.util.AppLogger

/**
 * ActionExecutor
 * Executes AgentAction commands
 */
class ActionExecutor(private val service: OperitAccessibilityService) {
    
    companion object {
        private const val TAG = "ActionExecutor"
    }
    
    private val finger = Finger(service)
    private val perception = Perception(service)
    
    /**
     * Execute an action string
     */
    fun execute(actionString: String): Boolean {
        AppLogger.d(TAG, "Executing action: $actionString")
        
        val parts = actionString.split(":", limit = 2)
        val actionType = parts.getOrNull(0) ?: actionString
        val actionParam = parts.getOrNull(1)
        
        return when (actionType) {
            "tap" -> executeTap(actionParam)
            "click" -> executeClick(actionParam)
            "longPress" -> executeLongPress(actionParam)
            "swipe" -> executeSwipe(actionParam)
            "scrollDown" -> finger.scrollDown()
            "scrollUp" -> finger.scrollUp()
            "back" -> finger.pressBack()
            "home" -> finger.pressHome()
            "input" -> executeInput(actionParam)
            "wait" -> executeWait(actionParam)
            else -> {
                AppLogger.w(TAG, "Unknown action: $actionType")
                false
            }
        }
    }
    
    private fun executeTap(param: String?): Boolean {
        return when (param) {
            "center" -> {
                val (width, height) = service.getScreenSize()
                finger.tap(width / 2, height / 2)
            }
            else -> {
                // Try to parse as coordinates
                val coords = param?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                if (coords != null && coords.size >= 2) {
                    finger.tap(coords[0], coords[1])
                } Try to find element else {
                    // by text and click
                    val element = perception.findByText(param ?: "")
                    if (element != null) {
                        finger.tap(element)
                    } else {
                        false
                    }
                }
            }
        }
    }
    
    private fun executeClick(param: String?): Boolean {
        return when (param) {
            null -> finger.tap(service.getScreenSize().first / 2, service.getScreenSize().second / 2)
            else -> {
                val coords = param.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (coords.size >= 2) {
                    finger.tap(coords[0], coords[1])
                } else {
                    val element = perception.findByText(param)
                    if (element != null) {
                        finger.tap(element)
                    } else {
                        false
                    }
                }
            }
        }
    }
    
    private fun executeLongPress(param: String?): Boolean {
        return when (param) {
            "center" -> {
                val (width, height) = service.getScreenSize()
                finger.longPress(width / 2, height / 2)
            }
            else -> {
                val coords = param?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
                if (coords != null && coords.size >= 2) {
                    finger.longPress(coords[0], coords[1])
                } else {
                    false
                }
            }
        }
    }
    
    private fun executeSwipe(param: String?): Boolean {
        return when (param) {
            "up" -> finger.swipeUp()
            "down" -> finger.swipeDown()
            "left" -> finger.swipeLeft()
            "right" -> finger.swipeRight()
            else -> {
                val coords = param?.split(";")?.map { it.split(",").mapNotNull { c -> c.trim().toIntOrNull() } }
                if (coords != null && coords.size >= 2 && coords[0].size >= 2 && coords[1].size >= 2) {
                    finger.swipe(coords[0][0], coords[0][1], coords[1][0], coords[1][1])
                } else {
                    false
                }
            }
        }
    }
    
    private fun executeInput(param: String?): Boolean {
        if (param == null) return false
        return finger.inputText(param)
    }
    
    private fun executeWait(param: String?): Boolean {
        val duration = param?.toLongOrNull() ?: 1000
        Thread.sleep(duration)
        return true
    }
}
