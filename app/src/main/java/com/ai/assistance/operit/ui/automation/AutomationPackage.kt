package com.ai.assistance.operit.ui.automation

/**
 * Operit UI Automation Package
 * 
 * This package provides comprehensive UI automation capabilities modeled after the Blurr Android app.
 * 
 * ## Core Components
 * 
 * ### ScreenInteractionService
 * The core AccessibilityService that provides:
 * - UI hierarchy dump and parsing
 * - Gesture execution (tap, long press, swipe, scroll)
 * - Text input into focused fields
 * - Global device actions (back, home, recents)
 * - Screenshot capture
 * 
 * ### Eyes API
 * High-level screen reading interface:
 * - openEyes() - Capture screenshot
 * - openXMLEyes() - Get UI hierarchy as readable format
 * - openPureXMLEyes() - Get UI hierarchy as XML
 * - getRawScreenData() - Get screen data with scroll info
 * - getKeyBoardStatus() - Check if keyboard is open
 * 
 * ### Finger API
 * High-level gesture execution interface:
 * - tap(x, y) - Tap at coordinates
 * - longPress(x, y) - Long press at coordinates
 * - swipe(x1, y1, x2, y2, duration) - Swipe between points
 * - scrollUp(pixels), scrollDown(pixels) - Scroll gestures
 * - type(text) - Input text
 * - back(), home(), switchApp() - Navigation
 * 
 * ### SemanticParser
 * Parses UI hierarchy into structured data:
 * - parseNodeTree() - Convert AccessibilityNodeInfo to ScreenAnalysis
 * - Find elements by text, resource ID, class name
 * - Identify interactive elements
 * 
 * ### Perception
 * Screen analysis module:
 * - analyze() - Get complete screen state
 * - getScreenshot() - Capture screen bitmap
 * - getXmlHierarchy() - Get UI structure
 * 
 * ### Action System
 * - AgentAction - Sealed classes for all possible actions
 * - ActionExecutor - Executes actions on device
 * - AgentOutput - LLM response structure
 * 
 * ### Agent
 * SENSE-THINK-ACT loop orchestrator:
 * - run(task) - Execute automation task
 * - Memory management and prompt construction
 * - Integration with LLM for decision making
 * 
 * ## Usage
 * 
 * ```kotlin
 * // Get screen analysis
 * val perception = context.getPerception()
 * val analysis = perception.analyze()
 * 
 * // Execute an action
 * val finger = context.getFinger()
 * finger.tap(500, 500)
 * 
 * // Run automation task
 * val agent = Agent(context)
 * val result = agent.run("Open settings and change theme")
 * ```
 */
class AutomationPackage
