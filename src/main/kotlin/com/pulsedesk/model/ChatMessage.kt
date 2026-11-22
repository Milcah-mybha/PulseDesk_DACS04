package com.pulsedesk.model

enum class MessageSenderType {
    CUSTOMER,
    AGENT,
    SYSTEM
}

data class FileAttachment(
    val fileName: String,
    val fileSize: String = "2.4 MB",
    val isVerified: Boolean = true,
    val verificationHash: String = "#ash256207283843639299a"
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderType: MessageSenderType,
    val message: String,
    val timestamp: String,
    val isDelivered: Boolean = true,
    val fileAttachment: FileAttachment? = null
)
