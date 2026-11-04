package com.pulsedesk.app.data.repository

import com.pulsedesk.app.data.local.ConversationDao
import com.pulsedesk.app.data.local.ConversationEntity
import com.pulsedesk.app.model.Conversation
import com.pulsedesk.app.model.ConversationCategory
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

/**
 * Repository: Nguồn dữ liệu duy nhất (SSOT) theo kiến trúc MVVM.
 * Kết hợp lưu trữ cục bộ Room Database và luồng mạng TCP Socket.
 */
class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val socketManager: SocketClientManager = SocketClientManager(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    // Flow danh sách hội thoại từ Room Database tự động phát khi dữ liệu thay đổi
    val conversations: Flow<List<Conversation>> = conversationDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    val socketState: StateFlow<SocketConnectionState> = socketManager.connectionState
    val socketEvents: SharedFlow<SocketEvent> = socketManager.events

    init {
        scope.launch {
            seedInitialDataIfEmpty()
            observeSocketPackets()
        }
    }

    fun start() {
        socketManager.start()
    }

    fun stop() {
        socketManager.stop()
    }

    // Lắng nghe gói tin Socket trên Dispatchers.IO và lưu thẳng vào Room Database
    private fun observeSocketPackets() {
        scope.launch(Dispatchers.IO) {
            socketManager.events.collect { event ->
                if (event is SocketEvent.PacketReceived) {
                    handleIncomingPacket(event.packet)
                }
            }
        }
    }

    suspend fun handleIncomingPacket(packet: SocketMessagePacket) = withContext(Dispatchers.IO) {
        val existing = conversationDao.getById(packet.conversationId)
        val now = System.currentTimeMillis()

        if (existing != null) {
            val newUnread = packet.unreadCount ?: (existing.unreadCount + 1)
            conversationDao.updateMessage(
                id = packet.conversationId,
                message = packet.message ?: existing.lastMessage,
                unreadCount = newUnread,
                timestamp = packet.timestamp ?: "Vừa xong",
                updatedAt = now
            )
        } else if (packet.message != null) {
            val newEntity = ConversationEntity(
                id = packet.conversationId,
                customerName = "Khách hàng #${packet.conversationId}",
                avatarInitials = "KH",
                topic = "Tư vấn trực tuyến",
                lastMessage = packet.message,
                timestamp = packet.timestamp ?: "Vừa xong",
                unreadCount = packet.unreadCount ?: 1,
                isOnline = true,
                updatedAt = now
            )
            conversationDao.insert(newEntity)
        }
    }

    suspend fun markAsRead(conversationId: String) = withContext(Dispatchers.IO) {
        conversationDao.markAsRead(conversationId)
    }

    // Khởi tạo dữ liệu ban đầu vào Room nếu cơ sở dữ liệu trống
    private suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (conversationDao.count() == 0) {
            val initial = listOf(
                Conversation(
                    id = "conv_1",
                    customerName = "Nguyễn Hoàng Nam",
                    avatarInitials = "HN",
                    avatarColorIndex = 0,
                    topic = "Hỗ trợ kỹ thuật",
                    lastMessage = "Hệ thống báo lỗi 502 khi tôi xuất hóa đơn VAT, nhờ team hỗ trợ gấp!",
                    timestamp = "10:48 AM",
                    unreadCount = 2,
                    isOnline = true,
                    isVip = true,
                    category = ConversationCategory.VIP
                ),
                Conversation(
                    id = "conv_2",
                    customerName = "Trần Thị Mai Anh",
                    avatarInitials = "MA",
                    avatarColorIndex = 1,
                    topic = "Thanh toán & Gói cước",
                    lastMessage = "Mình đã chuyển khoản nâng cấp gói Enterprise 12 tháng rồi nhé.",
                    timestamp = "10:30 AM",
                    unreadCount = 1,
                    isOnline = true,
                    isVip = false,
                    category = ConversationCategory.IN_PROGRESS
                ),
                Conversation(
                    id = "conv_3",
                    customerName = "Lê Quốc Bảo (TechLead)",
                    avatarInitials = "QB",
                    avatarColorIndex = 2,
                    topic = "Tư vấn API nội bộ",
                    lastMessage = "Đã cập nhật webhook endpoint cho cổng thanh toán bên thứ ba.",
                    timestamp = "09:15 AM",
                    unreadCount = 0,
                    isOnline = false,
                    isVip = false,
                    category = ConversationCategory.ALL
                ),
                Conversation(
                    id = "conv_4",
                    customerName = "Phạm Quỳnh Như",
                    avatarInitials = "QN",
                    avatarColorIndex = 3,
                    topic = "Tư vấn Chatbot AI",
                    lastMessage = "Bên em có hỗ trợ tích hợp Chatbot AI vào CRM sẵn có không?",
                    timestamp = "08:50 AM",
                    unreadCount = 4,
                    isOnline = true,
                    isVip = true,
                    category = ConversationCategory.VIP
                ),
                Conversation(
                    id = "conv_5",
                    customerName = "Đặng Minh Trí",
                    avatarInitials = "MT",
                    avatarColorIndex = 4,
                    topic = "Bảo mật tài khoản",
                    lastMessage = "Cảm ơn bạn, tôi đã kích hoạt thành công xác thực 2 bước (2FA).",
                    timestamp = "Hôm qua",
                    unreadCount = 0,
                    isOnline = false,
                    isVip = false,
                    category = ConversationCategory.ALL
                )
            )
            conversationDao.insertAll(initial.map { ConversationEntity.fromDomain(it) })
        }
    }

    suspend fun simulateIncomingSocketMessage() = withContext(Dispatchers.IO) {
        val targetId = listOf("conv_1", "conv_2", "conv_4").random()
        val packet = SocketMessagePacket(
            type = "NEW_MESSAGE",
            conversationId = targetId,
            message = "Em vừa gửi chuyển khoản thành công, nhờ admin kích hoạt giúp em!",
            timestamp = "Vừa xong"
        )
        handleIncomingPacket(packet)
    }
}
