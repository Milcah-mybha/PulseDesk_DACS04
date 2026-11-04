package com.pulsedesk.app.data.local

import com.pulsedesk.app.model.Conversation
import com.pulsedesk.app.model.ConversationCategory

data class ConversationEntity(
    val id: String,
    val customerName: String,
    val avatarInitials: String,
    val avatarColorIndex: Int = 0,
    val topic: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isVip: Boolean = false,
    val category: String = "ALL",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        customerName = customerName,
        avatarInitials = avatarInitials,
        avatarColorIndex = avatarColorIndex,
        topic = topic,
        lastMessage = lastMessage,
        timestamp = timestamp,
        unreadCount = unreadCount,
        isOnline = isOnline,
        isVip = isVip,
        category = try {
            ConversationCategory.valueOf(category)
        } catch (_: Exception) {
            ConversationCategory.ALL
        }
    )

    companion object {
        fun fromDomain(conv: Conversation, updatedAt: Long = System.currentTimeMillis()): ConversationEntity =
            ConversationEntity(
                id = conv.id,
                customerName = conv.customerName,
                avatarInitials = conv.avatarInitials,
                avatarColorIndex = conv.avatarColorIndex,
                topic = conv.topic,
                lastMessage = conv.lastMessage,
                timestamp = conv.timestamp,
                unreadCount = conv.unreadCount,
                isOnline = conv.isOnline,
                isVip = conv.isVip,
                category = conv.category.name,
                updatedAt = updatedAt
            )
    }
}
