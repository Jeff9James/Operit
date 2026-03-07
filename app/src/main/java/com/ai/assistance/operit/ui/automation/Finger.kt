package com.ai.assistance.operit.ui.automation

import android.content.Context

/**
 * Finger API - Gesture execution wrapper
 * Provides high-level methods for device interaction
 * Based on Blurr's Finger implementation
 */
class Finger(private val context: Context) {
    
    private val service: ScreenInteractionService?
        get() = ScreenInteractionService.instance
    
    // ==================== Gesture Methods ====================
    
    /**
     * Tap at specific coordinates
     */
    fun tap(x: Int, y: Int): Boolean {
        return service?.clickOnPoint(x.toFloat(), y.toFloat()) ?: false
    }
    
    /**
     * Long press at specific coordinates
     */
    fun longPress(x: Int, y: Int): Boolean {
        return service?.longClickOnPoint(x.toFloat(), y.toFloat()) ?: false
    }
    
    /**
     * Swipe between two points
     * @param x1 Start X coordinate
     * @param y1 Start Y coordinate
     * @param x2 End X coordinate
     * @param y2 End Y coordinate
     * @param duration Duration in milliseconds (default 1000ms)
     */
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int = 1000): Boolean {
        return service?.swipe(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), duration.toLong()) ?: false
    }
    
    /**
     * Type text into focused input field
     * Also calls enter after typing
     */
    fun type(text: String): Boolean {
        val result = service?.typeTextInFocusedField(text) ?: false
        if (result) {
            enter()
        }
        return result
    }
    
    /**
     * Press enter key
     */
    fun enter(): Boolean {
        return service?.pressEnter() ?: false
    }
    
    // ==================== Global Actions ====================
    
    /**
     * Navigate back
     */
    fun back(): Boolean {
        return service?.performBack() ?: false
    }
    
    /**
     * Navigate to home screen
     */
    fun home(): Boolean {
        return service?.performHome() ?: false
    }
    
    /**
     * Open app switcher (recents)
     */
    fun switchApp(): Boolean {
        return service?.performRecents() ?: false
    }
    
    /**
     * Expand notifications panel
     */
    fun notifications(): Boolean {
        return service?.expandNotifications() ?: false
    }
    
    /**
     * Open power menu
     */
    fun powerMenu(): Boolean {
        return service?.openPowerMenu() ?: false
    }
    
    // ==================== Scroll Methods ====================
    
    /**
     * Scroll up by specified pixels
     * @param pixels Number of pixels to scroll
     * @param duration Duration in milliseconds
     */
    fun scrollUp(pixels: Int = 500, duration: Int = 500): Boolean {
        val screenHeight = getScreenHeight()
        return swipe(
            screenWidth / 2,
            screenHeight / 2 + pixels / 2,
            screenWidth / 2,
            screenHeight / 2 - pixels / 2,
            duration
        )
    }
    
    /**
     * Scroll down by specified pixels
     * @param pixels Number of pixels to scroll
     * @param duration Duration in milliseconds
     */
    fun scrollDown(pixels: Int = 500, duration: Int = 500): Boolean {
        val screenHeight = getScreenHeight()
        return swipe(
            screenWidth / 2,
            screenHeight / 2 - pixels / 2,
            screenWidth / 2,
            screenHeight / 2 + pixels / 2,
            duration
        )
    }
    
    /**
     * Scroll left by specified pixels
     */
    fun scrollLeft(pixels: Int = 500, duration: Int = 500): Boolean {
        val screenWidth = getScreenWidth()
        return swipe(
            screenWidth / 2 + pixels / 2,
            screenHeight / 2,
            screenWidth / 2 - pixels / 2,
            screenHeight / 2,
            duration
        )
    }
    
    /**
     * Scroll right by specified pixels
     */
    fun scrollRight(pixels: Int = 500, duration: Int = 500): Boolean {
        val screenWidth = getScreenWidth()
        return swipe(
            screenWidth / 2 - pixels / 2,
            screenHeight / 2,
            screenWidth / 2 + pixels / 2,
            screenHeight / 2,
            duration
        )
    }
    
    /**
     * Scroll down precisely using physics-based scrolling
     */
    fun scrollDownPrecisely(pixels: Int, pixelsPerSecond: Int = 1000): Boolean {
        return service?.scrollDownPrecisely(pixels, pixelsPerSecond) ?: false
    }
    
    /**
     * Scroll up precisely
     */
    fun scrollUpPrecisely(pixels: Int, pixelsPerSecond: Int = 1000): Boolean {
        return service?.scrollUpPrecisely(pixels, pixelsPerSecond) ?: false
    }
    
    // ==================== Utility Methods ====================
    
    private fun getScreenWidth(): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels
    }
    
    private fun getScreenHeight(): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.heightPixels
    }
}

/**
 * Extension function to get Finger instance
 */
fun Context.getFinger(): Finger = Finger(this)
