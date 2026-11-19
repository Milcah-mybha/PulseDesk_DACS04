package com.pulsedesk.model

enum class MessageSenderType {
    CUSTOMER,
    AGENT,
    SYSTEM
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderType: MessageSenderType,
    val message: String,
    val timestamp: String,
    val isDelivered: Boolean = true
)
