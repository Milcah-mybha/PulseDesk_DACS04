package com.pulsedesk.model

data class RagCitation(
    val title: String,
    val section: String,
    val score: Float
)

data class AiAssistantMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: String,
    val citations: List<RagCitation> = emptyList()
)
