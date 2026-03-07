package com.ai.assistance.operit.ui.automation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Perception Module - Screen Analysis
 * Observes device screen and creates structured analysis
 * Based on Blurr's Perception implementation
 */
class Perception(private val context: Context) {
    
    private val eyes: Eyes = Eyes(context)
    private val semanticParser: SemanticParser = SemanticParser()
    
    /**
     * Analyze current screen state
     * Returns structured ScreenAnalysis with UI representation and element map
     */
    suspend fun analyze(): ScreenAnalysis = withContext(Dispatchers.IO) {
        // Gather screen data concurrently
        val rawScreenDataDeferred = async { eyes.getRawScreenData() }
        val keyboardStatusDeferred = async { eyes.getKeyBoardStatus() }
        val activityNameDeferred = async { eyes.getCurrentActivityName() }
        
        val rawScreenData = rawScreenDataDeferred.await()
        val keyboardOpen = keyboardStatusDeferred.await()
        val activityName = activityNameDeferred.await() ?: ""
        
        if (rawScreenData == null) {
            return@withContext ScreenAnalysis(
                uiRepresentation = "Unable to get screen data",
                elementMap = emptyMap(),
                keyboardOpen = keyboardOpen,
                activityName = activityName,
                pixelsAbove = 0,
                pixelsBelow = 0,
                screenWidth = 0,
                screenHeight = 0
            )
        }
        
        // Parse the UI hierarchy using SemanticParser
        val screenAnalysis = semanticParser.parseNodeTree(rawScreenData.rootNode)
        
        rawScreenData.rootNode.recycle()
        
        ScreenAnalysis(
            uiRepresentation = screenAnalysis.uiRepresentation,
            elementMap = screenAnalysis.elementMap,
            keyboardOpen = keyboardOpen,
            activityName = activityName,
            pixelsAbove = rawScreenData.pixelsAbove,
            pixelsBelow = rawScreenData.pixelsBelow,
            screenWidth = rawScreenData.screenWidth,
            screenHeight = rawScreenData.screenHeight
        )
    }
    
    /**
     * Get screenshot bitmap
     */
    suspend fun getScreenshot(): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        eyes.openEyes()
    }
    
    /**
     * Get UI hierarchy as XML
     */
    suspend fun getXmlHierarchy(): String = withContext(Dispatchers.IO) {
        eyes.openXMLEyes()
    }
    
    /**
     * Get pure XML hierarchy
     */
    suspend fun getPureXmlHierarchy(): String = withContext(Dispatchers.IO) {
        eyes.openPureXMLEyes()
    }
    
    /**
     * Get current activity name
     */
    fun getCurrentActivityName(): String? {
        return eyes.getCurrentActivityName()
    }
    
    /**
     * Check if keyboard is open
     */
    fun isKeyboardOpen(): Boolean {
        return eyes.getKeyBoardStatus()
    }
    
    /**
     * Get element map for action execution
     */
    suspend fun getElementMap(): Map<Int, InteractiveElement> {
        val analysis = analyze()
        return analysis.elementMap
    }
}

/**
 * Extension function to get Perception instance
 */
fun Context.getPerception(): Perception = Perception(this)
