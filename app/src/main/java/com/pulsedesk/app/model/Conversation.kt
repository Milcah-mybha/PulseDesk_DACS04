package com.pulsedesk.app.model

data class Conversation(
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
    val category: ConversationCategory = ConversationCategory.ALL
)

enum class ConversationCategory(val displayName: String) {
    ALL("Tất cả"),
    UNREAD("Chưa đọc"),
    VIP("Khách VIP"),
    IN_PROGRESS("Đang xử lý")
}

data class SocketMessagePacket(
    val type: String,
    val conversationId: String,
    val message: String? = null,
    val unreadCount: Int? = null,
    val timestamp: String? = null,
    val isOnline: Boolean? = null
)
