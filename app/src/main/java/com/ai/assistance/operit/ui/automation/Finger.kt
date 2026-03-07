package com.ai.assistance.operit.ui.automation

import com.ai.assistance.operit.services.OperitAccessibilityService
import com.ai.assistance.operit.util.AppLogger

/**
 * Finger - High-level API for gesture execution
 * Provides tap, swipe, scroll, and other gesture functions
 */
class Finger(private val service: OperitAccessibilityService) {
    
    companion object {
        private const val TAG = "Finger"
        
        /**
         * Get singleton instance
         */
        fun getInstance(): Finger? {
            val service = OperitAccessibilityService.getInstance()
            return service?.let { Finger(it) }
        }
    }
    
    private val screenSize: Pair<Int, Int>
        get() = service.getScreenSize()
    
    /**
     * Tap at coordinates
     */
    fun tap(x: Int, y: Int): Boolean {
        AppLogger.d(TAG, "Tap at ($x, $y)")
        return service.performClickAt(x, y)
    }
    
    /**
     * Tap at element center
     */
    fun tap(element: InteractiveElement): Boolean {
        val centerX = element.bounds[0] + element.bounds[2] / 2
        val centerY = element.bounds[1] + element.bounds[3] / 2
        return tap(centerX, centerY)
    }
    
    /**
     * Long press at coordinates
     */
    fun longPress(x: Int, y: Int): Boolean {
        AppLogger.d(TAG, "Long press at ($x, $y)")
        return service.performLongPressAt(x, y)
    }
    
    /**
     * Long press at element center
     */
    fun longPress(element: InteractiveElement): Boolean {
        val centerX = element.bounds[0] + element.bounds[2] / 2
        val centerY = element.bounds[1] + element.bounds[3] / 2
        return longPress(centerX, centerY)
    }
    
    /**
     * Swipe from (x1, y1) to (x2, y2)
     */
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Long = 300): Boolean {
        AppLogger.d(TAG, "Swipe from ($x1, $y1) to ($x2, $y2) duration=$duration")
        return service.performSwipeGesture(x1, y1, x2, y2, duration)
    }
    
    /**
     * Swipe up
     */
    fun swipeUp(duration: Long = 300): Boolean {
        val (width, height) = screenSize
        val startY = (height * 0.8).toInt()
        val endY = (height * 0.2).toInt()
        val centerX = width / 2
        return swipe(centerX, startY, centerX, endY, duration)
    }
    
    /**
     * Swipe down
     */
    fun swipeDown(duration: Long = 300): Boolean {
        val (width, height) = screenSize
        val startY = (height * 0.2).toInt()
        val endY = (height * 0.8).toInt()
        val centerX = width / 2
        return swipe(centerX, startY, centerX, endY, duration)
    }
    
    /**
     * Swipe left
     */
    fun swipeLeft(duration: Long = 300): Boolean {
        val (width, height) = screenSize
        val startX = (width * 0.8).toInt()
        val endX = (width * 0.2).toInt()
        val centerY = height / 2
        return swipe(startX, centerY, endX, centerY, duration)
    }
    
    /**
     * Swipe right
     */
    fun swipeRight(duration: Long = 300): Boolean {
        val (width, height) = screenSize
        val startX = (width * 0.2).toInt()
        val endX = (width * 0.8).toInt()
        val centerY = height / 2
        return swipe(startX, centerY, endX, centerY, duration)
    }
    
    /**
     * Scroll down
     */
    fun scrollDown(): Boolean {
        AppLogger.d(TAG, "Scroll down")
        return service.performScrollDown()
    }
    
    /**
     * Scroll up
     */
    fun scrollUp(): Boolean {
        AppLogger.d(TAG, "Scroll up")
        return service.performScrollUp()
    }
    
    /**
     * Input text into focused field
     */
    fun inputText(text: String): Boolean {
        AppLogger.d(TAG, "Input text: $text")
        return service.performTextInput(text)
    }
    
    /**
     * Press back
     */
    fun pressBack(): Boolean {
        AppLogger.d(TAG, "Press back")
        return service.performBack()
    }
    
    /**
     * Press home
     */
    fun pressHome(): Boolean {
        AppLogger.d(TAG, "Press home")
        return service.performHome()
    }
    
    /**
     * Open recents
     */
    fun openRecents(): Boolean {
        AppLogger.d(TAG, "Open recents")
        return service.performRecents()
    }
    
    /**
     * Open notifications
     */
    fun openNotifications(): Boolean {
        AppLogger.d(TAG, "Open notifications")
        return service.performNotifications()
    }
    
    /**
     * Open power dialog
     */
    fun openPowerDialog(): Boolean {
        AppLogger.d(TAG, "Open power dialog")
        return service.performPowerDialog()
    }
}
