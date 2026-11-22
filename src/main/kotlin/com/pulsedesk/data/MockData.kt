package com.pulsedesk.data

import com.pulsedesk.model.*

object MockData {
    val conversations = listOf(
        Conversation(
            id = "conv_1",
            customerName = "Tran Van Anh",
            avatarInitials = "VA",
            avatarColorIndex = 0,
            topic = "Billing Issue #9876",
            lastMessage = "Billing Issue #9876. Least more ...",
            timestamp = "3m ago",
            unreadCount = 1,
            isOnline = true,
            isVip = false,
            category = ConversationCategory.IN_PROGRESS
        ),
        Conversation(
            id = "conv_2",
            customerName = "Le Thi Binh",
            avatarInitials = "LB",
            avatarColorIndex = 1,
            topic = "Product Return Request",
            lastMessage = "Latest messages. Now ligting do...",
            timestamp = "1h ago",
            unreadCount = 1,
            isOnline = true,
            isVip = false,
            category = ConversationCategory.IN_PROGRESS
        ),
        Conversation(
            id = "conv_3",
            customerName = "Le Thi Binh",
            avatarInitials = "LB",
            avatarColorIndex = 2,
            topic = "Product Return Request",
            lastMessage = "Product Return, Request, Anoks...",
            timestamp = "1h ago",
            unreadCount = 1,
            isOnline = false,
            isVip = false,
            category = ConversationCategory.ALL
        ),
        Conversation(
            id = "conv_4",
            customerName = "Le Thi Binh",
            avatarInitials = "LB",
            avatarColorIndex = 3,
            topic = "Billing Issue #9876",
            lastMessage = "Billing Issue #9876, Billiniojssue...",
            timestamp = "3m ago",
            unreadCount = 1,
            isOnline = false,
            isVip = false,
            category = ConversationCategory.ALL
        )
    )

    fun getMessagesFor(conversationId: String): List<ChatMessage> {
        return when (conversationId) {
            "conv_2" -> listOf(
                ChatMessage(
                    id = "msg_1",
                    conversationId = conversationId,
                    senderType = MessageSenderType.CUSTOMER,
                    message = "Hello, I am having an issue with the product refund. Can you help?",
                    timestamp = "10:30 AM"
                ),
                ChatMessage(
                    id = "msg_2",
                    conversationId = conversationId,
                    senderType = MessageSenderType.AGENT,
                    message = "Hi Binh! Yes, I can certainly check that refund for you. Let me view your transaction.",
                    timestamp = "10:32 AM"
                ),
                ChatMessage(
                    id = "msg_3",
                    conversationId = conversationId,
                    senderType = MessageSenderType.CUSTOMER,
                    message = "",
                    timestamp = "10:35 AM",
                    fileAttachment = FileAttachment(
                        fileName = "return_receipt.pdf",
                        fileSize = "2.4 MB",
                        isVerified = true,
                        verificationHash = "#ash256207283843639299a"
                    )
                ),
                ChatMessage(
                    id = "msg_4",
                    conversationId = conversationId,
                    senderType = MessageSenderType.AGENT,
                    message = "Let me check the document you sent.",
                    timestamp = "10:36 AM"
                ),
                ChatMessage(
                    id = "msg_5",
                    conversationId = conversationId,
                    senderType = MessageSenderType.CUSTOMER,
                    message = "Here is the PDF of the return receipt...",
                    timestamp = "10:38 AM"
                )
            )
            else -> listOf(
                ChatMessage(
                    id = "msg_default_1",
                    conversationId = conversationId,
                    senderType = MessageSenderType.CUSTOMER,
                    message = "Hello, I am having an issue with the product refund. Can you help?",
                    timestamp = "10:30 AM"
                ),
                ChatMessage(
                    id = "msg_default_2",
                    conversationId = conversationId,
                    senderType = MessageSenderType.AGENT,
                    message = "Hi Binh! Yes, I can certainly check that refund for you. Let me view your transaction.",
                    timestamp = "10:32 AM"
                )
            )
        }
    }

    val knowledgeBase = listOf(
        KnowledgeItem(
            id = "kb_1",
            title = "Quy trình đối soát hoàn tiền và xử lý đơn hàng lỗi",
            category = "Thanh toán & Hóa đơn",
            summary = "Hướng dẫn chi tiết các bước tiếp nhận yêu cầu hoàn tiền của khách hàng, đối soát chứng từ hóa đơn và xác thực chữ ký số SHA-256.",
            tags = listOf("refund", "billing", "receipt"),
            lastUpdated = "2 ngày trước",
            viewsCount = 1420
        ),
        KnowledgeItem(
            id = "kb_2",
            title = "Chính sách đổi trả sản phẩm (Product Return Request)",
            category = "Chính sách SME",
            summary = "Điều kiện đổi trả trong vòng 15 ngày, yêu cầu biên nhận hóa đơn PDF gốc và xác minh số sê-ri thiết bị.",
            tags = listOf("return", "product", "policy"),
            lastUpdated = "Hôm qua",
            viewsCount = 980
        ),
        KnowledgeItem(
            id = "kb_3",
            title = "Cấu hình cổng API Webhook TCP Socket",
            category = "Kỹ thuật",
            summary = "Kết nối real-time giữa client desktop và server quản trị CSKH qua cổng 8888, đồng bộ tin nhắn 2 chiều.",
            tags = listOf("socket", "tcp", "api"),
            lastUpdated = "Tuần trước",
            viewsCount = 560
        )
    )

    val aiAssistantInitialMessages = listOf(
        AiAssistantMessage(
            id = "ai_1",
            isUser = false,
            text = "Xin chào! Tôi là Trợ lý AI PulseDesk hỗ trợ tra cứu chính sách CSKH và tra soát dữ liệu SME. Bạn cần giải đáp thông tin gì?",
            timestamp = "09:00 AM"
        )
    )
}
