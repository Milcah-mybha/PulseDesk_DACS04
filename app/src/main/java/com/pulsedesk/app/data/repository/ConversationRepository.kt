package com.pulsedesk.app.data.repository

import com.pulsedesk.app.data.local.ChatMessageDao
import com.pulsedesk.app.data.local.ChatMessageEntity
import com.pulsedesk.app.data.local.ConversationDao
import com.pulsedesk.app.data.local.ConversationEntity
import com.pulsedesk.app.model.ChatMessage
import com.pulsedesk.app.model.Conversation
import com.pulsedesk.app.model.ConversationCategory
import com.pulsedesk.app.model.MessageDeliveryStatus
import com.pulsedesk.app.model.MessageSenderType
import com.pulsedesk.app.model.SocketMessagePacket
import com.pulsedesk.app.network.SocketClientManager
import com.pulsedesk.app.network.SocketConnectionState
import com.pulsedesk.app.network.SocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao,
    private val socketManager: SocketClientManager = SocketClientManager(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val conversations: Flow<List<Conversation>> = conversationDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }
    val socketState: StateFlow<SocketConnectionState> = socketManager.connectionState
    val socketEvents: SharedFlow<SocketEvent> = socketManager.events

    init {
        scope.launch {
            seedInitialDataIfEmpty()
            seedAllMessagesIfEmpty()
            observeSocketPackets()
        }
    }

    private suspend fun seedAllMessagesIfEmpty() = withContext(Dispatchers.IO) {
        listOf("conv_1", "conv_2", "conv_3", "conv_4", "conv_5").forEach { convId ->
            seedMessagesIfEmpty(convId)
        }
    }

    fun start() = socketManager.start()
    fun stop() = socketManager.stop()

    fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        scope.launch(Dispatchers.IO) {
            seedMessagesIfEmpty(conversationId)
        }
        return chatMessageDao.getMessagesByConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    // Gửi tin nhắn từ nhân viên CSKH (AGENT)
    suspend fun sendMessage(conversationId: String, text: String): Boolean = withContext(Dispatchers.IO) {
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val msgId = UUID.randomUUID().toString()

        val chatMsg = ChatMessage(
            id = msgId,
            conversationId = conversationId,
            senderType = MessageSenderType.AGENT,
            message = text,
            timestamp = timeNow,
            status = MessageDeliveryStatus.SENT,
            createdAt = System.currentTimeMillis()
        )

        // 1. Lưu tin nhắn vào SQLite cục bộ
        chatMessageDao.insert(ChatMessageEntity.fromDomain(chatMsg))

        // 2. Cập nhật hội thoại
        conversationDao.updateMessage(
            id = conversationId,
            message = text,
            unreadCount = 0,
            timestamp = timeNow,
            updatedAt = System.currentTimeMillis()
        )

        // 3. Gửi gói tin qua TCP Socket 8888
        val payload = JSONObject().apply {
            put("type", "AGENT_MESSAGE")
            put("conversationId", conversationId)
            put("message", text)
            put("timestamp", timeNow)
        }.toString()

        socketManager.sendMessage(payload)
    }

    private fun observeSocketPackets() {
        scope.launch(Dispatchers.IO) {
            socketManager.events.collect { event ->
                if (event is SocketEvent.PacketReceived) handleIncomingPacket(event.packet)
            }
        }
    }

    suspend fun handleIncomingPacket(packet: SocketMessagePacket) = withContext(Dispatchers.IO) {
        val existing = conversationDao.getById(packet.conversationId)
        val now = System.currentTimeMillis()
        val timeDisplay = packet.timestamp ?: "Vừa xong"

        if (existing != null) {
            val newUnread = packet.unreadCount ?: (existing.unreadCount + 1)
            conversationDao.updateMessage(
                id = packet.conversationId,
                message = packet.message ?: existing.lastMessage,
                unreadCount = newUnread,
                timestamp = timeDisplay,
                updatedAt = now
            )
        } else if (packet.message != null) {
            conversationDao.insert(
                ConversationEntity(
                    id = packet.conversationId,
                    customerName = "Khách hàng #${packet.conversationId}",
                    avatarInitials = "KH",
                    topic = "Tư vấn trực tuyến",
                    lastMessage = packet.message,
                    timestamp = timeDisplay,
                    unreadCount = packet.unreadCount ?: 1,
                    isOnline = true,
                    updatedAt = now
                )
            )
        }

        // Lưu tin nhắn nhận được vào lịch sử chat
        if (!packet.message.isNullOrBlank()) {
            val msgEntity = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = packet.conversationId,
                senderType = MessageSenderType.CUSTOMER.name,
                message = packet.message,
                timestamp = timeDisplay,
                status = MessageDeliveryStatus.DELIVERED.name,
                createdAt = now
            )
            chatMessageDao.insert(msgEntity)
        }
    }

    suspend fun markAsRead(id: String) = withContext(Dispatchers.IO) {
        conversationDao.markAsRead(id)
    }

    private suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (conversationDao.count() == 0) {
            val initial = listOf(
                Conversation("conv_1", "Nguyễn Hoàng Nam", "HN", 0, "Hỗ trợ kỹ thuật", "Hệ thống báo lỗi 502 khi tôi xuất hóa đơn VAT, nhờ team hỗ trợ gấp!", "10:48 AM", 2, isOnline = true, isVip = true, category = ConversationCategory.VIP),
                Conversation("conv_2", "Trần Thị Mai Anh", "MA", 1, "Thanh toán & Gói cước", "Mình đã chuyển khoản nâng cấp gói Enterprise 12 tháng rồi nhé.", "10:30 AM", 1, isOnline = true, isVip = false, category = ConversationCategory.IN_PROGRESS),
                Conversation("conv_3", "Lê Quốc Bảo (TechLead)", "QB", 2, "Tư vấn API nội bộ", "Đã cập nhật webhook endpoint cho cổng thanh toán bên thứ ba.", "09:15 AM", 0, isOnline = false, isVip = false, category = ConversationCategory.ALL),
                Conversation("conv_4", "Phạm Quỳnh Như", "QN", 3, "Tư vấn Chatbot AI", "Bên em có hỗ trợ tích hợp Chatbot AI vào CRM sẵn có không?", "08:50 AM", 4, isOnline = true, isVip = true, category = ConversationCategory.VIP),
                Conversation("conv_5", "Đặng Minh Trí", "MT", 4, "Bảo mật tài khoản", "Cảm ơn bạn, tôi đã kích hoạt thành công xác thực 2 bước (2FA).", "Hôm qua", 0, isOnline = false, isVip = false, category = ConversationCategory.ALL)
            )
            conversationDao.insertAll(initial.map { ConversationEntity.fromDomain(it) })
        }
    }

    private suspend fun seedMessagesIfEmpty(convId: String) = withContext(Dispatchers.IO) {
        if (chatMessageDao.countByConversation(convId) == 0) {
            val baseTime = System.currentTimeMillis() - 1000 * 60 * 15
            val mockMessages = when (convId) {
                "conv_1" -> listOf(
                    ChatMessageEntity(UUID.randomUUID().toString(), convId, MessageSenderType.CUSTOMER.name, "Chào đội ngũ hỗ trợ PulseDesk!", "10:30 AM", MessageDeliveryStatus.READ.name, baseTime),
                    ChatMessageEntity(UUID.randomUUID().toString(), convId, MessageSenderType.AGENT.name, "Dạ em chào anh Nam, em có thể hỗ trợ gì cho anh hôm nay ạ?", "10:32 AM", MessageDeliveryStatus.READ.name, baseTime + 60000),
                    ChatMessageEntity(UUID.randomUUID().toString(), convId, MessageSenderType.CUSTOMER.name, "Hệ thống báo lỗi 502 khi tôi xuất hóa đơn VAT, nhờ team hỗ trợ gấp!", "10:48 AM", MessageDeliveryStatus.DELIVERED.name, baseTime + 120000)
                )
                "conv_2" -> listOf(
                    ChatMessageEntity(UUID.randomUUID().toString(), convId, MessageSenderType.CUSTOMER.name, "Bên mình còn áp dụng chương trình giảm 20% gói Enterprise không bạn?", "10:15 AM", MessageDeliveryStatus.READ.name, baseTime),
                    ChatMessageEntity(UUID.randomUUID().toString(), convId, MessageSenderType.AGENT.name, "Dạ vẫn còn ưu đãi đến hết tuần này chị nhé!", "10:18 AM", MessageDeliveryStatus.READ.name, baseTime + 60000),
                    ChatMessageEntity(UUID.randomUUID().toString(), convId, MessageSenderType.CUSTOMER.name, "Mình đã chuyển khoản nâng cấp gói Enterprise 12 tháng rồi nhé.", "10:30 AM", MessageDeliveryStatus.DELIVERED.name, baseTime + 120000)
                )
                else -> listOf(
                    ChatMessageEntity(UUID.randomUUID().toString(), convId, MessageSenderType.CUSTOMER.name, "Xin chào, tôi cần hỗ trợ thông tin dịch vụ.", "09:00 AM", MessageDeliveryStatus.READ.name, baseTime),
                    ChatMessageEntity(UUID.randomUUID().toString(), convId, MessageSenderType.AGENT.name, "Dạ vâng, xin mời bạn gửi yêu cầu để bên mình giải đáp ngay ạ.", "09:05 AM", MessageDeliveryStatus.READ.name, baseTime + 60000)
                )
            }
            chatMessageDao.insertAll(mockMessages)
        }
    }

    suspend fun simulateIncomingSocketMessage() = withContext(Dispatchers.IO) {
        val targetId = listOf("conv_1", "conv_2", "conv_4").random()
        handleIncomingPacket(
            SocketMessagePacket(
                type = "NEW_MESSAGE",
                conversationId = targetId,
                message = "Em vừa gửi chuyển khoản thành công, nhờ admin kích hoạt giúp em!",
                timestamp = "Vừa xong"
            )
        )
    }
}
