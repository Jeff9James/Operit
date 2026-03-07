package com.ai.assistance.operit.ui.automation

/**
 * Agent Output - LLM Response Structure
 * Represents the structured response from the LLM for a single step
 * Based on Blurr's AgentOutput implementation
 */
data class AgentOutput(
    val thinking: String = "",
    val evaluationPreviousGoal: String = "",
    val memory: String = "",
    val nextGoal: String = "",
    val actions: List<AgentAction> = emptyList()
)
