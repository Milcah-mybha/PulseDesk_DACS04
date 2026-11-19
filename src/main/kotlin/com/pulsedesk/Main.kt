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
import com.pulsedesk.model.MessageSenderType
import com.pulsedesk.ui.components.*
import com.pulsedesk.ui.screens.*
import com.pulsedesk.ui.theme.PulseDeskTheme
import com.pulsedesk.ui.theme.TextDim

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
    // 1. Trạng thái điều hướng chính (Chats, AI Assistant, Settings)
    var currentNavTab by remember { mutableStateOf(DesktopNavTab.CHATS) }

    // 2. Trạng thái tab phân loại ("Active Chats" và "Knowledge Base")
    var currentMainTab by remember { mutableStateOf(MainContentTab.ACTIVE_CHATS) }

    // 3. Trạng thái tìm kiếm & bộ lọc
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ConversationCategory.ALL) }

    // 4. Dữ liệu hội thoại mẫu
    val conversationList = remember { mutableStateListOf(*MockData.conversations.toTypedArray()) }
    var selectedConversationId by remember { mutableStateOf<String?>("conv_1") }

    // 5. Lịch sử tin nhắn của hội thoại hiện tại
    val chatMessages = remember {
        mutableStateMapOf<String, MutableList<ChatMessage>>().apply {
            MockData.conversations.forEach { conv ->
                put(conv.id, MockData.getMessagesFor(conv.id).toMutableList())
            }
        }
    }

    val selectedConversation = conversationList.find { it.id == selectedConversationId }
    val activeMessages = selectedConversationId?.let { chatMessages[it] } ?: emptyList()

    // Lọc danh sách hội thoại
    val filteredConversations = conversationList.filter { conv ->
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

    PulseDeskRootBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Yêu cầu 3: Thanh tiêu đề (App Bar) với PulseDesk, avatar nhân viên & chấm xanh Online phát sáng
            TopAppBar(
                agentName = "Nguyễn CSKH (Agent #01)",
                isTcpOnline = true
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

                                        // Cột 2: Khung chi tiết cuộc trò chuyện và gửi tin nhắn
                                        ChatDetailPane(
                                            conversation = selectedConversation,
                                            messages = activeMessages,
                                            onSendMessage = { text ->
                                                selectedConversationId?.let { convId ->
                                                    val newMsg = ChatMessage(
                                                        id = System.currentTimeMillis().toString(),
                                                        conversationId = convId,
                                                        senderType = MessageSenderType.AGENT,
                                                        message = text,
                                                        timestamp = "Vừa xong"
                                                    )
                                                    val list = chatMessages.getOrPut(convId) { mutableListOf() }
                                                    list.add(newMsg)
                                                    // Cập nhật tin nhắn mới nhất
                                                    val idx = conversationList.indexOfFirst { it.id == convId }
                                                    if (idx != -1) {
                                                        conversationList[idx] = conversationList[idx].copy(
                                                            lastMessage = text,
                                                            timestamp = "Vừa xong"
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                MainContentTab.KNOWLEDGE_BASE -> {
                                    // Yêu cầu 4 (Tab 2): Màn hình cơ sở tri thức nội bộ
                                    KnowledgeBaseScreen(
                                        articles = MockData.knowledgeBase,
                                        searchQuery = searchQuery,
                                        onArticleClick = { /* Chi tiết bài viết */ }
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
                        SettingsScreen()
                    }
                }
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
