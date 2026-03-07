package com.ai.assistance.operit.ui.automation

import android.view.accessibility.AccessibilityNodeInfo
import com.ai.assistance.operit.util.AppLogger

/**
 * InteractiveElement
 * Represents a parsed UI element with its properties
 */
data class InteractiveElement(
    val id: Int,
    val text: String,
    val contentDescription: String,
    val resourceId: String,
    val className: String,
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isEnabled: Boolean,
    val isVisibleToUser: Boolean,
    val bounds: IntArray,
    val depth: Int,
    val parentId: Int?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as InteractiveElement
        return id == other.id
    }
    
    override fun hashCode(): Int = id
}

/**
 * UIAnalysis
 * Contains the result of parsing the UI hierarchy
 */
data class UIAnalysis(
    val elementMap: Map<Int, InteractiveElement>,
    val uiRepresentation: String
)

/**
 * SemanticParser
 * Parses AccessibilityNodeInfo into structured InteractiveElement data
 */
class SemanticParser {
    companion object {
        private const val TAG = "SemanticParser"
        private var idCounter = 0
        
        fun resetIdCounter() {
            idCounter = 0
        }
    }
    
    /**
     * Parse node tree into UIAnalysis
     */
    fun parseNodeTree(rootNode: AccessibilityNodeInfo): UIAnalysis {
        resetIdCounter()
        val elementMap = mutableMapOf<Int, InteractiveElement>()
        val sb = StringBuilder()
        
        parseNode(rootNode, elementMap, sb, 0, null)
        
        return UIAnalysis(elementMap.toMap(), sb.toString())
    }
    
    private fun parseNode(
        node: AccessibilityNodeInfo,
        elementMap: MutableMap<Int, InteractiveElement>,
        sb: StringBuilder,
        depth: Int,
        parentId: Int?
    ) {
        if (!node.isVisibleToUser) return
        
        val id = idCounter++
        
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val resourceIdStr = node.viewIdResourceName ?: ""
        val classNameStr = node.className?.toString() ?: ""
        
        val bounds = IntArray(4)
        node.getBoundsInScreen(bounds)
        
        val element = InteractiveElement(
            id = id,
            text = text,
            contentDescription = contentDesc,
            resourceId = resourceIdStr,
            className = classNameStr,
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            isEditable = node.isEditable,
            isCheckable = node.isCheckable,
            isChecked = node.isChecked,
            isEnabled = node.isEnabled,
            isVisibleToUser = node.isVisibleToUser,
            bounds = bounds,
            depth = depth,
            parentId = parentId
        )
        
        elementMap[id] = element
        
        // Add to string representation
        val indent = "  ".repeat(depth)
        val clickable = if (node.isClickable) " [clickable]" else ""
        val scrollable = if (node.isScrollable) " [scrollable]" else ""
        val editable = if (node.isEditable) " [editable]" else ""
        
        val displayText = when {
            text.isNotBlank() -> text
            contentDesc.isNotBlank() -> "[$contentDesc]"
            else -> ""
        }
        
        if (displayText.isNotBlank() || node.isClickable || node.isScrollable) {
            sb.appendLine("$indent- $classNameStr: $displayText (id: $resourceIdStr)$clickable$scrollable$editable")
        }
        
        // Parse children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                parseNode(child, elementMap, sb, depth + 1, id)
                child.recycle()
            }
        }
    }
    
    /**
     * Find element by ID
     */
    fun findElementById(analysis: UIAnalysis, resourceId: String): InteractiveElement? {
        return analysis.elementMap.values.find { it.resourceId == resourceId }
    }
    
    /**
     * Find element by text
     */
    fun findElementByText(analysis: UIAnalysis, text: String): InteractiveElement? {
        return analysis.elementMap.values.find { it.text.contains(text, ignoreCase = true) }
    }
    
    /**
     * Find clickable elements
     */
    fun findClickableElements(analysis: UIAnalysis): List<InteractiveElement> {
        return analysis.elementMap.values.filter { it.isClickable }
    }
    
    /**
     * Find scrollable elements
     */
    fun findScrollableElements(analysis: UIAnalysis): List<InteractiveElement> {
        return analysis.elementMap.values.filter { it.isScrollable }
    }
}
