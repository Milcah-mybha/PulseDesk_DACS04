package com.pulsedesk.data

import com.pulsedesk.model.*

object MockData {
    val conversations = listOf(
        Conversation(
            id = "conv_1",
            customerName = "Nguyễn Hoàng Nam",
            avatarInitials = "HN",
            avatarColorIndex = 0,
            topic = "Lỗi xuất hóa đơn VAT #8492",
            lastMessage = "Hệ thống báo lỗi 502 khi tôi xuất hóa đơn VAT, nhờ team hỗ trợ gấp!",
            timestamp = "10:48 AM",
            unreadCount = 2,
            isOnline = true,
            isVip = true,
            category = ConversationCategory.VIP,
            customerEmail = "nam.nguyen@techcorp.vn",
            customerPhone = "0912 345 678",
            planTier = "Enterprise Plus"
        ),
        Conversation(
            id = "conv_2",
            customerName = "Trần Thị Mai Anh",
            avatarInitials = "MA",
            avatarColorIndex = 1,
            topic = "Thanh toán & Gói cước Enterprise",
            lastMessage = "Mình đã chuyển khoản nâng cấp gói Enterprise 12 tháng rồi nhé.",
            timestamp = "10:30 AM",
            unreadCount = 1,
            isOnline = true,
            isVip = false,
            category = ConversationCategory.IN_PROGRESS,
            customerEmail = "maianh@vietretail.com",
            customerPhone = "0987 654 321",
            planTier = "SME Growth"
        ),
        Conversation(
            id = "conv_3",
            customerName = "Lê Quốc Bảo (TechLead)",
            avatarInitials = "QB",
            avatarColorIndex = 2,
            topic = "Tích hợp Webhook API cổng thanh toán",
            lastMessage = "Đã cập nhật webhook endpoint cho cổng thanh toán bên thứ ba.",
            timestamp = "09:15 AM",
            unreadCount = 0,
            isOnline = false,
            isVip = false,
            category = ConversationCategory.ALL,
            customerEmail = "bao.le@fintechvn.io",
            customerPhone = "0903 112 233",
            planTier = "Developer API"
        ),
        Conversation(
            id = "conv_4",
            customerName = "Phạm Quỳnh Như",
            avatarInitials = "QN",
            avatarColorIndex = 3,
            topic = "Tư vấn Chatbot AI đa kênh CRM",
            lastMessage = "Bên em có hỗ trợ tích hợp Chatbot AI vào CRM sẵn có không?",
            timestamp = "08:50 AM",
            unreadCount = 4,
            isOnline = true,
            isVip = true,
            category = ConversationCategory.VIP,
            customerEmail = "nhu.pham@beautybox.vn",
            customerPhone = "0978 998 877",
            planTier = "OmniChannel VIP"
        ),
        Conversation(
            id = "conv_5",
            customerName = "Đặng Minh Trí",
            avatarInitials = "MT",
            avatarColorIndex = 4,
            topic = "Kích hoạt bảo mật 2 lớp 2FA",
            lastMessage = "Cảm ơn bạn, tôi đã kích hoạt thành công xác thực 2 bước (2FA).",
            timestamp = "Hôm qua",
            unreadCount = 0,
            isOnline = false,
            isVip = false,
            category = ConversationCategory.ALL,
            customerEmail = "tri.dang@logisticpro.vn",
            customerPhone = "0934 556 677",
            planTier = "SME Basic"
        )
    )

    fun getMessagesFor(conversationId: String): List<ChatMessage> {
        return when (conversationId) {
            "conv_1" -> listOf(
                ChatMessage("m1", conversationId, MessageSenderType.CUSTOMER, "Chào đội ngũ hỗ trợ PulseDesk!", "10:30 AM"),
                ChatMessage("m2", conversationId, MessageSenderType.AGENT, "Dạ em chào anh Nam, em có thể hỗ trợ gì cho anh hôm nay ạ?", "10:32 AM"),
                ChatMessage("m3", conversationId, MessageSenderType.CUSTOMER, "Hệ thống báo lỗi 502 khi tôi xuất hóa đơn VAT, nhờ team hỗ trợ gấp!", "10:48 AM")
            )
            "conv_2" -> listOf(
                ChatMessage("m4", conversationId, MessageSenderType.CUSTOMER, "Bên mình còn ưu đãi giảm 20% gói Enterprise không bạn?", "10:15 AM"),
                ChatMessage("m5", conversationId, MessageSenderType.AGENT, "Dạ vẫn còn ưu đãi đến hết tuần này chị nhé!", "10:18 AM"),
                ChatMessage("m6", conversationId, MessageSenderType.CUSTOMER, "Mình đã chuyển khoản nâng cấp gói Enterprise 12 tháng rồi nhé.", "10:30 AM")
            )
            else -> listOf(
                ChatMessage("m7", conversationId, MessageSenderType.CUSTOMER, "Xin chào PulseDesk, tôi cần tư vấn dịch vụ.", "09:00 AM"),
                ChatMessage("m8", conversationId, MessageSenderType.AGENT, "Dạ PulseDesk sẵn sàng giải đáp mọi thắc mắc của bạn ngay ạ!", "09:05 AM")
            )
        }
    }

    val knowledgeBase = listOf(
        KnowledgeItem(
            id = "kb_1",
            title = "Quy trình xử lý sự cố lỗi kết nối cổng thanh toán Napas/VietQR",
            category = "Kỹ thuật",
            summary = "Các bước kiểm tra IP Whitelist, chứng chỉ SSL mTLS và mã phản hồi 502/504 từ gateway trung gian.",
            tags = listOf("Payment", "API", "SSL", "VietQR"),
            lastUpdated = "18/11/2026",
            viewsCount = 1420
        ),
        KnowledgeItem(
            id = "kb_2",
            title = "Chính sách nâng cấp & chiết khấu gói cước SME Pro lên Enterprise",
            category = "Kinh doanh",
            summary = "Khung giảm giá 15-25% cho hợp đồng thanh toán từ 12 tháng trở lên, kèm gói tư vấn Chatbot AI riêng biệt.",
            tags = listOf("Pricing", "Enterprise", "Discount"),
            lastUpdated = "15/11/2026",
            viewsCount = 980
        ),
        KnowledgeItem(
            id = "kb_3",
            title = "Hướng dẫn thiết lập xác thực 2 bước (2FA) & phân quyền vai trò Agent",
            category = "Bảo mật",
            summary = "Yêu cầu bảo mật cấp công ty: TOTP qua Google Authenticator hoặc YubiKey đối với tài khoản CSKH cấp cao.",
            tags = listOf("Security", "2FA", "RBAC"),
            lastUpdated = "12/11/2026",
            viewsCount = 654
        ),
        KnowledgeItem(
            id = "kb_4",
            title = "Cấu hình Webhook nhận tin nhắn tự động từ Zalo OA và Fanpage",
            category = "Tích hợp",
            summary = "Tài liệu kỹ thuật đồng bộ tin nhắn thời gian thực về PulseDesk qua TCP Socket nội bộ port 8888.",
            tags = listOf("ZaloOA", "Facebook", "Webhook", "TCP"),
            lastUpdated = "08/11/2026",
            viewsCount = 2100
        )
    )

    val aiAssistantInitialMessages = listOf(
        AiAssistantMessage(
            id = "ai_1",
            isUser = false,
            text = "Xin chào! Tôi là Trợ lý AI PulseDesk Copilot (RAG Engine). Tôi có thể giúp bạn tra cứu quy trình nội bộ, giải đáp chính sách gói cước hoặc tóm tắt sự cố của khách hàng. Hãy hỏi tôi bất cứ điều gì!",
            timestamp = "Vừa xong"
        ),
        AiAssistantMessage(
            id = "ai_2",
            isUser = true,
            text = "Khách hàng gặp lỗi 502 khi xuất hóa đơn VAT thì xử lý thế nào theo quy trình công ty?",
            timestamp = "10:49 AM"
        ),
        AiAssistantMessage(
            id = "ai_3",
            isUser = false,
            text = "Theo tài liệu kỹ thuật KB-01 về Cổng thanh toán & Hóa đơn điện tử, lỗi 502 thường do nghẽn kết nối tại gateway cơ quan Thuế. Bạn hãy hướng dẫn khách hàng:\n1. Kiểm tra lại mã số thuế và định dạng XML.\n2. Chờ 60 giây và thử lại qua hàng đợi dự phòng (Queue B).\n3. Nếu vẫn lỗi, yêu cầu mã hóa đơn tạm để kỹ thuật đối soát trực tiếp.",
            timestamp = "10:49 AM",
            citations = listOf(
                RagCitation("KB-01: Quy trình xử lý lỗi kết nối hóa đơn", "Mục 3.2: Sự cố gateway 502", 0.94f),
                RagCitation("SLA-2026: Cam kết thời gian xử lý sự cố VAT", "Trang 4", 0.88f)
            )
        )
    )
}
