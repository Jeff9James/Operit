package com.ai.assistance.operit.ui.automation

/**
 * Action Sealed Classes - AI Agent Actions
 * These represent the actions the AI agent can execute
 * Based on Blurr's Action implementation
 */

// Base action interface
interface AgentAction

/**
 * Tap on element by ID
 */
data class TapElement(val elementId: Int) : AgentAction

/**
 * Long press on element by ID
 */
data class LongPressElement(val elementId: Int) : AgentAction

/**
 * Input text into focused field
 */
data class InputText(val text: String) : AgentAction

/**
 * Tap element, input text, and press enter
 */
data class TapElementInputTextPressEnter(val elementId: Int, val text: String) : AgentAction

/**
 * Speak message to user
 */
data class Speak(val message: String) : AgentAction

/**
 * Ask user a question
 */
data class Ask(val question: String) : AgentAction

/**
 * Open app by name
 */
data class OpenApp(val appName: String) : AgentAction

/**
 * Navigate back
 */
data object Back : AgentAction

/**
 * Navigate to home
 */
data object Home : AgentAction

/**
 * Open app switcher
 */
data object SwitchApp : AgentAction

/**
 * Wait for specified duration
 */
data class Wait(val durationSeconds: Int = 5) : AgentAction

/**
 * Scroll down by amount
 */
data class ScrollDown(val amount: Int) : AgentAction

/**
 * Scroll up by amount
 */
data class ScrollUp(val amount: Int) : AgentAction

/**
 * Search Google
 */
data class SearchGoogle(val query: String) : AgentAction

/**
 * Task completion signal
 */
data class Done(
    val success: Boolean,
    val text: String,
    val filesToDisplay: List<String>? = null
) : AgentAction

/**
 * Append to file
 */
data class AppendFile(val fileName: String, val content: String) : AgentAction

/**
 * Read file
 */
data class ReadFile(val fileName: String) : AgentAction

/**
 * Write to file
 */
data class WriteFile(val fileName: String, val content: String) : AgentAction

/**
 * Launch Android intent
 */
data class LaunchIntent(val intentName: String, val parameters: Map<String, String>) : AgentAction

/**
 * Take screenshot
 */
data object TakeScreenshot : AgentAction

/**
 * Open URL
 */
data class OpenUrl(val url: String) : AgentAction

/**
 * Copy text to clipboard
 */
data class CopyToClipboard(val text: String) : AgentAction
