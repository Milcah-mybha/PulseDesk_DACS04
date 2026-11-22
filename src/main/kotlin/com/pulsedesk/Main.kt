package com.pulsedesk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pulsedesk.data.MockData
import com.pulsedesk.model.ChatMessage
import com.pulsedesk.model.Conversation
import com.pulsedesk.model.ConversationCategory
import com.pulsedesk.model.KnowledgeItem
import com.pulsedesk.model.MessageSenderType
import com.pulsedesk.network.PulseSocketClient
import com.pulsedesk.ui.components.*
import com.pulsedesk.ui.screens.*
import com.pulsedesk.ui.theme.PulseDeskTheme
import com.pulsedesk.ui.theme.TextDim
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1260.dp, 840.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "PulseDesk - Hệ thống CSKH & Tư vấn nội bộ SME",
        state = windowState
    ) {
        PulseDeskTheme {
            PulseDeskApp()
        }
    }
}

@Composable
fun PulseDeskApp() {
    val scope = rememberCoroutineScope()

    // 1. Trạng thái điều hướng chính (Chats, AI Assistant, Settings)
    var currentNavTab by remember { mutableStateOf(DesktopNavTab.CHATS) }

    // 2. Trạng thái tab phân loại ("Active Chats" và "Knowledge Base")
    var currentMainTab by remember { mutableStateOf(MainContentTab.ACTIVE_CHATS) }

    // 3. Trạng thái tìm kiếm & bộ lọc
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ConversationCategory.ALL) }

    // 4. Trạng thái kết nối TCP Socket & Cấu hình hệ thống
    var serverHost by remember { mutableStateOf("127.0.0.1") }
    var serverPort by remember { mutableStateOf("8888") }
    var agentName by remember { mutableStateOf("Nguyễn CSKH (Agent #01)") }
    var isTcpOnline by remember { mutableStateOf(false) }

    // 5. Trạng thái khách hàng đang gõ & tự động phản hồi
    var isCustomerTyping by remember { mutableStateOf(false) }
    var autoReplyEnabled by remember { mutableStateOf(true) }

    // 6. Trạng thái giao diện mở rộng: Hồ sơ khách hàng & Xem chi tiết bài viết tri thức
    var showCustomerInfoPanel by remember { mutableStateOf(true) }
    var selectedKbArticle by remember { mutableStateOf<KnowledgeItem?>(null) }

    // 6. Dữ liệu hội thoại mẫu
    val conversationList = remember { mutableStateListOf(*MockData.conversations.toTypedArray()) }
    var selectedConversationId by remember { mutableStateOf<String?>(MockData.conversations.firstOrNull()?.id) }

    // 7. Lịch sử tin nhắn được quan sát theo danh sách bất biến để recomposition luôn hoạt động
    val chatMessages = remember {
        mutableStateMapOf<String, List<ChatMessage>>().apply {
            MockData.conversations.forEach { conv ->
                put(conv.id, MockData.getMessagesFor(conv.id))
            }
        }
    }

    // 8. Tích hợp PulseSocketClient kết nối TCP Server thực tế (Port 8888)
    val socketClient = remember {
        PulseSocketClient(
            onMessageReceived = { msg ->
                val current = chatMessages[msg.conversationId] ?: emptyList()
                chatMessages[msg.conversationId] = current + msg

                val idx = conversationList.indexOfFirst { it.id == msg.conversationId }
                if (idx != -1) {
                    val isCurrentChat = msg.conversationId == selectedConversationId
                    conversationList[idx] = conversationList[idx].copy(
                        lastMessage = msg.message,
                        timestamp = msg.timestamp,
                        unreadCount = if (isCurrentChat || msg.senderType == MessageSenderType.AGENT) 0 else conversationList[idx].unreadCount + 1
                    )
                }
            },
            onConnectionStatusChanged = { status ->
                isTcpOnline = status
            }
        )
    }

    // Thử kết nối TCP Socket ngầm
    LaunchedEffect(Unit) {
        socketClient.connect(host = "127.0.0.1", port = 8888)
    }

    DisposableEffect(Unit) {
        onDispose {
            socketClient.disconnect()
        }
    }

    // Hàm tạo câu trả lời tự động thông minh của khách hàng theo ngữ cảnh
    fun generateSimulatedCustomerReply(agentText: String, conversation: Conversation): String {
        val lower = agentText.lowercase()
        return when {
            lower.contains("kiểm tra") || lower.contains("đang xem") || lower.contains("chờ") ->
                "Dạ vâng em cảm ơn anh, nhờ anh kiểm tra giúp em xem giao dịch đã hoàn tất chưa nhé."
            lower.contains("mã") || lower.contains("giao dịch") || lower.contains("bill") ->
                "Dạ mã giao dịch của em là #SME-${(1000..9999).random()}, anh tra cứu trên cổng thanh toán giúp em với nha."
            lower.contains("khắc phục") || lower.contains("xong") || lower.contains("thành công") ->
                "Tuyệt vời quá! Em vào thử lại thì đã thấy hoạt động bình thường rồi ạ. Cảm ơn đội ngũ PulseDesk nhiều nhé!"
            lower.contains("giá") || lower.contains("chiết khấu") || lower.contains("gói") || lower.contains("hợp đồng") ->
                "Gói bên mình nếu ký hợp đồng 1 năm thì có được tặng thêm tháng dịch vụ hoặc hỗ trợ triển khai API không anh?"
            lower.contains("xin chào") || lower.contains("chào") || lower.contains("ạ") ->
                "Dạ chào anh, em đang cần hỗ trợ gấp về phần cấu hình tài khoản doanh nghiệp SME ạ."
            else -> {
                val sampleReplies = listOf(
                    "Dạ em đã nhận được thông tin phản hồi từ anh rồi ạ.",
                    "Vâng anh, vậy bên kỹ thuật xử lý xong nhớ báo lại em qua đây nhé.",
                    "Dạ em hiểu rồi, em sẽ chuyển thông tin này cho phòng kế toán đối chiếu ngay.",
                    "Cảm ơn sự hỗ trợ nhiệt tình và nhanh chóng của PulseDesk ạ!",
                    "Dạ ngoài ra em muốn hỏi thêm chút về chính sách bảo mật 2 lớp cho nhân viên bên em nữa."
                )
                sampleReplies.random()
            }
        }
    }

    // Hàm xử lý gửi tin nhắn 2 chiều (Tư vấn viên <-> Khách hàng)
    fun handleSendMessage(text: String, senderType: MessageSenderType) {
        val convId = selectedConversationId ?: return
        val currentConv = conversationList.find { it.id == convId } ?: return
        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val newMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = convId,
            senderType = senderType,
            message = text,
            timestamp = nowTime
        )

        val currentList = chatMessages[convId] ?: emptyList()
        chatMessages[convId] = currentList + newMsg

        // Cập nhật danh sách hội thoại
        val idx = conversationList.indexOfFirst { it.id == convId }
        if (idx != -1) {
            conversationList[idx] = conversationList[idx].copy(
                lastMessage = text,
                timestamp = nowTime,
                unreadCount = if (senderType == MessageSenderType.CUSTOMER && convId != selectedConversationId) {
                    conversationList[idx].unreadCount + 1
                } else 0
            )
        }

        // Bắn tin nhắn qua TCP Socket nếu đang kết nối
        socketClient.sendMessage(convId, senderType, text)

        // Nếu gửi từ Tư vấn viên (AGENT) và chế độ tự động phản hồi bật -> Giả lập khách hàng đang gõ và trả lời lại
        if (senderType == MessageSenderType.AGENT && autoReplyEnabled) {
            scope.launch {
                delay(800) // Đợi 800ms trước khi hiện đang gõ
                isCustomerTyping = true
                delay(1600) // Khách hàng soạn tin nhắn trong 1.6s
                isCustomerTyping = false

                val replyText = generateSimulatedCustomerReply(text, currentConv)
                val replyTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val customerMsg = ChatMessage(
                    id = "cust_${System.currentTimeMillis()}",
                    conversationId = convId,
                    senderType = MessageSenderType.CUSTOMER,
                    message = replyText,
                    timestamp = replyTime
                )

                val updatedList = chatMessages[convId] ?: emptyList()
                chatMessages[convId] = updatedList + customerMsg

                val cIdx = conversationList.indexOfFirst { it.id == convId }
                if (cIdx != -1) {
                    conversationList[cIdx] = conversationList[cIdx].copy(
                        lastMessage = replyText,
                        timestamp = replyTime
                    )
                }
            }
        }
    }

    val selectedConversation = conversationList.find { it.id == selectedConversationId }
    val activeMessages = selectedConversationId?.let { chatMessages[it] } ?: emptyList()

    // Lọc danh sách hội thoại tối ưu với derivedStateOf
    val filteredConversations by remember {
        derivedStateOf {
            conversationList.filter { conv ->
                val matchCat = when (selectedCategory) {
                    ConversationCategory.ALL -> true
                    ConversationCategory.UNREAD -> conv.unreadCount > 0
                    ConversationCategory.VIP -> conv.isVip
                    ConversationCategory.IN_PROGRESS -> conv.category == ConversationCategory.IN_PROGRESS
                }
                val matchQuery = searchQuery.isBlank() ||
                        conv.customerName.contains(searchQuery, true) ||
                        conv.topic.contains(searchQuery, true) ||
                        conv.lastMessage.contains(searchQuery, true)
                matchCat && matchQuery
            }
        }
    }

    PulseDeskRootBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Yêu cầu 3: Thanh tiêu đề (App Bar) với PulseDesk, avatar nhân viên & chấm xanh Online phát sáng
            PulseDeskTopAppBar(
                agentName = agentName,
                isTcpOnline = isTcpOnline
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Nội dung chính thay đổi theo thanh điều hướng
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentNavTab) {
                    DesktopNavTab.CHATS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Yêu cầu 4: Thanh tìm kiếm (Search Bar) và 2 tab phân loại
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassSearchBar(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    modifier = Modifier.weight(1.3f)
                                )

                                GlassMainCategoryTabs(
                                    selectedTab = currentMainTab,
                                    onTabSelected = { currentMainTab = it },
                                    activeChatsCount = conversationList.size,
                                    kbArticlesCount = MockData.knowledgeBase.size,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            when (currentMainTab) {
                                MainContentTab.ACTIVE_CHATS -> {
                                    // Bố cục Master-Detail Desktop cho Active Chats
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Cột 1: Danh sách hội thoại kính mờ dạng cuộn dọc
                                        Column(
                                            modifier = Modifier
                                                .width(360.dp)
                                                .fillMaxHeight()
                                        ) {
                                            // Bộ lọc phân loại (Tất cả, Chưa đọc, Khách VIP, Đang xử lý)
                                            GlassFilterPills(
                                                selectedCategory = selectedCategory,
                                                onCategorySelected = { selectedCategory = it }
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Yêu cầu 5: LazyColumn với các thẻ hội thoại Glassmorphism
                                            if (filteredConversations.isEmpty()) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Không tìm thấy cuộc hội thoại nào phù hợp.",
                                                        color = TextDim,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    items(
                                                        items = filteredConversations,
                                                        key = { it.id }
                                                    ) { conv ->
                                                        ConversationListCard(
                                                            conversation = conv,
                                                            isSelected = conv.id == selectedConversationId,
                                                            onClick = {
                                                                selectedConversationId = conv.id
                                                                // Đánh dấu đã đọc khi chọn
                                                                val index = conversationList.indexOfFirst { it.id == conv.id }
                                                                if (index != -1 && conversationList[index].unreadCount > 0) {
                                                                    conversationList[index] = conversationList[index].copy(unreadCount = 0)
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Cột 2: Khung chi tiết cuộc trò chuyện và gửi tin nhắn 2 chiều
                                        ChatDetailPane(
                                            conversation = selectedConversation,
                                            messages = activeMessages,
                                            isCustomerTyping = isCustomerTyping,
                                            autoReplyEnabled = autoReplyEnabled,
                                            showCustomerInfo = showCustomerInfoPanel,
                                            onToggleAutoReply = { autoReplyEnabled = !autoReplyEnabled },
                                            onToggleCustomerInfo = { showCustomerInfoPanel = !showCustomerInfoPanel },
                                            onSendMessage = { text, senderType ->
                                                handleSendMessage(text, senderType)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Cột 3: Bảng hồ sơ thông tin khách hàng doanh nghiệp SME
                                        if (showCustomerInfoPanel && selectedConversation != null) {
                                            CustomerInfoPanel(
                                                conversation = selectedConversation,
                                                onClose = { showCustomerInfoPanel = false }
                                            )
                                        }
                                    }
                                }

                                MainContentTab.KNOWLEDGE_BASE -> {
                                    // Yêu cầu 4 (Tab 2): Màn hình cơ sở tri thức nội bộ
                                    KnowledgeBaseScreen(
                                        articles = MockData.knowledgeBase,
                                        searchQuery = searchQuery,
                                        onArticleClick = { article ->
                                            selectedKbArticle = article
                                        }
                                    )
                                }
                            }
                        }
                    }

                    DesktopNavTab.AI_ASSISTANT -> {
                        // Yêu cầu 6 (Mục 2): AI Assistant (hỏi đáp RAG kèm nguồn tham khảo)
                        AiAssistantScreen()
                    }

                    DesktopNavTab.SETTINGS -> {
                        // Yêu cầu 6 (Mục 3): Settings (cấu hình IP/Port kết nối TCP Server)
                        SettingsScreen(
                            initialHost = serverHost,
                            initialPort = serverPort,
                            initialAgentName = agentName,
                            onSaveSettings = { host, port, name ->
                                serverHost = host
                                serverPort = port
                                agentName = name
                                val portNum = port.toIntOrNull() ?: 8888
                                socketClient.connect(host, portNum)
                            }
                        )
                    }
                }
            }

            // Hộp thoại xem chi tiết tài liệu cơ sở tri thức
            selectedKbArticle?.let { article ->
                KnowledgeDetailDialog(
                    article = article,
                    onDismiss = { selectedKbArticle = null },
                    onApplyToChat = { summaryText ->
                        handleSendMessage(summaryText, MessageSenderType.AGENT)
                        currentMainTab = MainContentTab.ACTIVE_CHATS
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Yêu cầu 6: Thanh điều hướng ở dưới gồm 3 mục (Chats, AI Assistant, Settings)
            GlassBottomNavBar(
                selectedTab = currentNavTab,
                onTabSelected = { currentNavTab = it }
            )
        }
    }
}
