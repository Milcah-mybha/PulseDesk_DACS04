package com.pulsedesk.viewmodel

import com.pulsedesk.data.local.DatabaseManager
import com.pulsedesk.model.*
import com.pulsedesk.network.PulseSocketClient
import com.pulsedesk.ui.components.DesktopNavTab
import com.pulsedesk.ui.components.MainContentTab
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PulseDeskViewModel(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    // 1. Trạng thái điều hướng
    private val _currentNavTab = MutableStateFlow(DesktopNavTab.CHATS)
    val currentNavTab: StateFlow<DesktopNavTab> = _currentNavTab.asStateFlow()

    private val _currentMainTab = MutableStateFlow(MainContentTab.ACTIVE_CHATS)
    val currentMainTab: StateFlow<MainContentTab> = _currentMainTab.asStateFlow()

    // 2. Trạng thái tìm kiếm & lọc
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ConversationCategory.ALL)
    val selectedCategory: StateFlow<ConversationCategory> = _selectedCategory.asStateFlow()

    // 3. Trạng thái danh sách hội thoại & tin nhắn
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _chatMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val chatMessages: StateFlow<Map<String, List<ChatMessage>>> = _chatMessages.asStateFlow()

    // 4. Trạng thái kết nối TCP & typing
    private val _isTcpOnline = MutableStateFlow(false)
    val isTcpOnline: StateFlow<Boolean> = _isTcpOnline.asStateFlow()

    private val _isCustomerTyping = MutableStateFlow(false)
    val isCustomerTyping: StateFlow<Boolean> = _isCustomerTyping.asStateFlow()

    private val _autoReplyEnabled = MutableStateFlow(true)
    val autoReplyEnabled: StateFlow<Boolean> = _autoReplyEnabled.asStateFlow()

    // 5. Cài đặt hệ thống
    private val _serverHost = MutableStateFlow(DatabaseManager.getSetting("server_host", "127.0.0.1"))
    val serverHost: StateFlow<String> = _serverHost.asStateFlow()

    private val _serverPort = MutableStateFlow(DatabaseManager.getSetting("server_port", "8888"))
    val serverPort: StateFlow<String> = _serverPort.asStateFlow()

    private val _agentName = MutableStateFlow(DatabaseManager.getSetting("agent_name", "Nguyễn CSKH (Agent #01)"))
    val agentName: StateFlow<String> = _agentName.asStateFlow()

    // 6. Bài viết Knowledge Base được chọn để xem chi tiết
    private val _selectedKbArticle = MutableStateFlow<KnowledgeItem?>(null)
    val selectedKbArticle: StateFlow<KnowledgeItem?> = _selectedKbArticle.asStateFlow()

    // 7. Bảng thông tin khách hàng (Drawer / Panel bên phải)
    private val _showCustomerInfoPanel = MutableStateFlow(true)
    val showCustomerInfoPanel: StateFlow<Boolean> = _showCustomerInfoPanel.asStateFlow()

    // TCP Socket Client
    private val socketClient = PulseSocketClient(
        onMessageReceived = { msg -> handleIncomingSocketMessage(msg) },
        onConnectionStatusChanged = { status -> _isTcpOnline.value = status }
    )

    init {
        loadDataFromDatabase()
        connectSocket()
    }

    private fun loadDataFromDatabase() {
        scope.launch(Dispatchers.IO) {
            val dbConvs = DatabaseManager.getAllConversations()
            val msgMap = mutableMapOf<String, List<ChatMessage>>()
            dbConvs.forEach { conv ->
                msgMap[conv.id] = DatabaseManager.getMessagesForConversation(conv.id)
            }
            _conversations.value = dbConvs
            _chatMessages.value = msgMap
            if (_selectedConversationId.value == null && dbConvs.isNotEmpty()) {
                _selectedConversationId.value = dbConvs.first().id
            }
        }
    }

    private fun connectSocket() {
        val port = _serverPort.value.toIntOrNull() ?: 8888
        socketClient.connect(_serverHost.value, port)
    }

    private fun handleIncomingSocketMessage(msg: ChatMessage) {
        scope.launch(Dispatchers.IO) {
            DatabaseManager.saveMessage(msg)
            val currentMap = _chatMessages.value.toMutableMap()
            val currentList = currentMap[msg.conversationId] ?: emptyList()
            currentMap[msg.conversationId] = currentList + msg
            _chatMessages.value = currentMap

            val list = _conversations.value.toMutableList()
            val idx = list.indexOfFirst { it.id == msg.conversationId }
            if (idx != -1) {
                val isCurrent = msg.conversationId == _selectedConversationId.value
                val updated = list[idx].copy(
                    lastMessage = msg.message,
                    timestamp = msg.timestamp,
                    unreadCount = if (isCurrent || msg.senderType == MessageSenderType.AGENT) 0 else list[idx].unreadCount + 1
                )
                list[idx] = updated
                _conversations.value = list
                DatabaseManager.updateConversation(updated)
            }
        }
    }

    fun selectConversation(id: String) {
        _selectedConversationId.value = id
        scope.launch(Dispatchers.IO) {
            DatabaseManager.markAsRead(id)
            val list = _conversations.value.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx != -1 && list[idx].unreadCount > 0) {
                val updated = list[idx].copy(unreadCount = 0)
                list[idx] = updated
                _conversations.value = list
                DatabaseManager.updateConversation(updated)
            }
        }
    }

    fun sendMessage(text: String, senderType: MessageSenderType) {
        val convId = _selectedConversationId.value ?: return
        val currentConv = _conversations.value.find { it.id == convId } ?: return
        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val newMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = convId,
            senderType = senderType,
            message = text,
            timestamp = nowTime
        )

        scope.launch(Dispatchers.IO) {
            DatabaseManager.saveMessage(newMsg)

            val currentMap = _chatMessages.value.toMutableMap()
            val currentList = currentMap[convId] ?: emptyList()
            currentMap[convId] = currentList + newMsg
            _chatMessages.value = currentMap

            val list = _conversations.value.toMutableList()
            val idx = list.indexOfFirst { it.id == convId }
            if (idx != -1) {
                val updated = list[idx].copy(
                    lastMessage = text,
                    timestamp = nowTime,
                    unreadCount = if (senderType == MessageSenderType.CUSTOMER && convId != _selectedConversationId.value) {
                        list[idx].unreadCount + 1
                    } else 0
                )
                list[idx] = updated
                _conversations.value = list
                DatabaseManager.updateConversation(updated)
            }

            // Gửi ra socket
            socketClient.sendMessage(convId, senderType, text)

            // Mô phỏng khách hàng phản hồi tự động nếu gửi từ Agent
            if (senderType == MessageSenderType.AGENT && _autoReplyEnabled.value) {
                launch {
                    delay(800)
                    _isCustomerTyping.value = true
                    delay(1600)
                    _isCustomerTyping.value = false

                    val replyText = generateCustomerReply(text)
                    val replyTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val custMsg = ChatMessage(
                        id = "cust_${System.currentTimeMillis()}",
                        conversationId = convId,
                        senderType = MessageSenderType.CUSTOMER,
                        message = replyText,
                        timestamp = replyTime
                    )

                    DatabaseManager.saveMessage(custMsg)
                    val map = _chatMessages.value.toMutableMap()
                    val msgs = map[convId] ?: emptyList()
                    map[convId] = msgs + custMsg
                    _chatMessages.value = map

                    val cList = _conversations.value.toMutableList()
                    val cIdx = cList.indexOfFirst { it.id == convId }
                    if (cIdx != -1) {
                        val cUpdated = cList[cIdx].copy(
                            lastMessage = replyText,
                            timestamp = replyTime
                        )
                        cList[cIdx] = cUpdated
                        _conversations.value = cList
                        DatabaseManager.updateConversation(cUpdated)
                    }
                }
            }
        }
    }

    private fun generateCustomerReply(agentText: String): String {
        val lower = agentText.lowercase()
        return when {
            lower.contains("kiểm tra") || lower.contains("chờ") ->
                "Dạ vâng em cảm ơn anh, nhờ anh kiểm tra giúp em xem giao dịch đã hoàn tất chưa nhé."
            lower.contains("mã") || lower.contains("giao dịch") || lower.contains("bill") ->
                "Dạ mã giao dịch của em là #SME-${(1000..9999).random()}, anh tra cứu trên cổng thanh toán giúp em với nha."
            lower.contains("khắc phục") || lower.contains("xong") || lower.contains("thành công") ->
                "Tuyệt vời quá! Em vào thử lại thì đã thấy hoạt động bình thường rồi ạ. Cảm ơn đội ngũ PulseDesk nhiều nhé!"
            lower.contains("giá") || lower.contains("chiết khấu") || lower.contains("gói") ->
                "Gói bên mình nếu ký hợp đồng 1 năm thì có được tặng thêm tháng dịch vụ hoặc hỗ trợ triển khai API không anh?"
            else -> {
                val list = listOf(
                    "Dạ em đã nhận được thông tin phản hồi từ anh rồi ạ.",
                    "Vâng anh, vậy bên kỹ thuật xử lý xong nhớ báo lại em qua đây nhé.",
                    "Dạ em hiểu rồi, em sẽ chuyển thông tin này cho phòng kế toán đối chiếu ngay.",
                    "Cảm ơn sự hỗ trợ nhiệt tình và nhanh chóng của PulseDesk ạ!"
                )
                list.random()
            }
        }
    }

    fun toggleAutoReply() {
        _autoReplyEnabled.value = !_autoReplyEnabled.value
    }

    fun toggleCustomerInfoPanel() {
        _showCustomerInfoPanel.value = !_showCustomerInfoPanel.value
    }

    fun selectKbArticle(article: KnowledgeItem?) {
        _selectedKbArticle.value = article
    }

    fun setNavTab(tab: DesktopNavTab) {
        _currentNavTab.value = tab
    }

    fun setMainTab(tab: MainContentTab) {
        _currentMainTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: ConversationCategory) {
        _selectedCategory.value = cat
    }

    fun saveSystemSettings(host: String, port: String, name: String) {
        _serverHost.value = host
        _serverPort.value = port
        _agentName.value = name
        scope.launch(Dispatchers.IO) {
            DatabaseManager.saveSetting("server_host", host)
            DatabaseManager.saveSetting("server_port", port)
            DatabaseManager.saveSetting("agent_name", name)
            connectSocket()
        }
    }

    fun cleanup() {
        socketClient.disconnect()
        scope.cancel()
    }
}
