package com.pulsedesk.app.data.local

import com.pulsedesk.app.model.ChatMessage
import com.pulsedesk.app.model.MessageDeliveryStatus
import com.pulsedesk.app.model.MessageSenderType

data class ChatMessageEntity(
    val id: String,
    val conversationId: String,
    val senderType: String,
    val message: String,
    val timestamp: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        conversationId = conversationId,
        senderType = try {
            MessageSenderType.valueOf(senderType)
        } catch (_: Exception) {
            MessageSenderType.CUSTOMER
        },
        message = message,
        timestamp = timestamp,
        status = try {
            MessageDeliveryStatus.valueOf(status)
        } catch (_: Exception) {
            MessageDeliveryStatus.SENT
        },
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(msg: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = msg.id,
            conversationId = msg.conversationId,
            senderType = msg.senderType.name,
            message = msg.message,
            timestamp = msg.timestamp,
            status = msg.status.name,
            createdAt = msg.createdAt
        )
    }
}
