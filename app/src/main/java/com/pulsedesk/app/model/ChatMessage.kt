package com.pulsedesk.app.model

enum class MessageSenderType {
    CUSTOMER,   // Khách hàng gửi đến
    AGENT,      // Nhân viên CSKH gửi đi
    SYSTEM      // Thông báo hệ thống / AI
}

enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderType: MessageSenderType,
    val message: String,
    val timestamp: String,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.SENT,
    val createdAt: Long = System.currentTimeMillis()
)
