package com.pulsedesk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pulsedesk.data.MockData
import com.pulsedesk.model.*
import com.pulsedesk.network.PulseSocketClient
import com.pulsedesk.ui.components.AppLogo
import com.pulsedesk.ui.components.DesktopNavTab
import com.pulsedesk.ui.components.MainContentTab
import com.pulsedesk.ui.components.PulseDeskRootBackground
import com.pulsedesk.ui.components.glassmorphism
import com.pulsedesk.ui.screens.*
import com.pulsedesk.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.pulsedesk.server.PulseDeskServerApplication
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

fun main() {
    // Tự động khởi động Spring Boot Backend Server (MongoDB Atlas + Socket 8888 + REST API) trong background
    try {
        val isAlreadyRunning = try {
            Socket().use { s ->
                s.connect(InetSocketAddress("127.0.0.1", 8888), 350)
                true
            }
        } catch (_: Exception) {
            false
        }

        if (!isAlreadyRunning) {
            thread(isDaemon = true, name = "EmbeddedPulseDeskServer") {
                try {
                    PulseDeskServerApplication.main(emptyArray())
                } catch (e: Exception) {
                    println("[PulseDesk] Embedded Server notice: ${e.message}")
                }
            }
        }
    } catch (_: Exception) {}

    application {
    val windowState = rememberWindowState(
        size = DpSize(1260.dp, 820.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "PulseDesk - Hệ thống CSKH & Tư vấn nội bộ SME",
        state = windowState
    ) {
        PulseDeskTheme {
            var isAuthenticated by remember { mutableStateOf(false) }
            var loggedStaffId by remember { mutableStateOf("staff_01@pulsedesk.vn") }
            var loggedUserRole by remember { mutableStateOf("Staff Member") }

            if (!isAuthenticated) {
                AuthScreen(
                    onLoginSuccess = { staffId, role ->
                        loggedStaffId = staffId
                        loggedUserRole = role
                        isAuthenticated = true
                    }
                )
            } else {
                PulseDeskApp(
                    staffId = loggedStaffId,
                    userRole = loggedUserRole,
                    onLogOut = { isAuthenticated = false }
                )
            }
        }
    }
}
}

@Composable
fun PulseDeskApp(
    staffId: String = "staff_01@pulsedesk.vn",
    userRole: String = "Staff Member",
    onLogOut: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    // 1. Trạng thái điều hướng chính (Chats, AI Assistant, Settings)
    var currentNavTab by remember { mutableStateOf(DesktopNavTab.CHATS) }

    // 2. Trạng thái tab phân loại ("Active Chats" và "Knowledge Base")
    var currentMainTab by remember { mutableStateOf(MainContentTab.ACTIVE_CHATS) }

    // 3. Trạng thái tìm kiếm
    var searchQuery by remember { mutableStateOf("") }

    // 4. Dữ liệu hội thoại mẫu (Mặc định chọn Le Thi Binh như trong ảnh)
    val conversationList = remember { mutableStateListOf(*MockData.conversations.toTypedArray()) }
    var selectedConversationId by remember { mutableStateOf<String?>("conv_2") }

    // 5. Lịch sử tin nhắn
    val chatMessages = remember {
        mutableStateMapOf<String, List<ChatMessage>>().apply {
            MockData.conversations.forEach { conv ->
                put(conv.id, MockData.getMessagesFor(conv.id))
            }
        }
    }

    // 6. Trạng thái Socket TCP & Khách hàng typing
    var isTcpOnline by remember { mutableStateOf(true) }
    var isCustomerTyping by remember { mutableStateOf(false) }
    var autoReplyEnabled by remember { mutableStateOf(true) }
    var selectedKbArticle by remember { mutableStateOf<KnowledgeItem?>(null) }

    // Socket Client
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
                        unreadCount = if (isCurrentChat) 0 else conversationList[idx].unreadCount + 1
                    )
                }
            },
            onConnectionStatusChanged = { status ->
                isTcpOnline = status
            }
        )
    }

    LaunchedEffect(Unit) {
        socketClient.connect(host = "127.0.0.1", port = 8888)
    }

    DisposableEffect(Unit) {
        onDispose {
            socketClient.disconnect()
        }
    }

    fun handleSendMessage(text: String, senderType: MessageSenderType) {
        val convId = selectedConversationId ?: return
        val nowTime = "Vừa xong"

        val newMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = convId,
            senderType = senderType,
            message = text,
            timestamp = nowTime
        )

        val currentList = chatMessages[convId] ?: emptyList()
        chatMessages[convId] = currentList + newMsg

        val idx = conversationList.indexOfFirst { it.id == convId }
        if (idx != -1) {
            conversationList[idx] = conversationList[idx].copy(
                lastMessage = text,
                timestamp = "Just now"
            )
        }

        socketClient.sendMessage(convId, senderType, text)

        // Phản hồi tự động
        if (senderType == MessageSenderType.AGENT && autoReplyEnabled) {
            scope.launch {
                delay(1200)
                val replyText = "Thank you! I will wait for your update."
                val customerMsg = ChatMessage(
                    id = "cust_${System.currentTimeMillis()}",
                    conversationId = convId,
                    senderType = MessageSenderType.CUSTOMER,
                    message = replyText,
                    timestamp = "Just now"
                )
                val updated = chatMessages[convId] ?: emptyList()
                chatMessages[convId] = updated + customerMsg

                val cIdx = conversationList.indexOfFirst { it.id == convId }
                if (cIdx != -1) {
                    conversationList[cIdx] = conversationList[cIdx].copy(
                        lastMessage = replyText,
                        timestamp = "Just now"
                    )
                }
            }
        }
    }

    val selectedConversation = conversationList.find { it.id == selectedConversationId }
    val activeMessages = selectedConversationId?.let { chatMessages[it] } ?: emptyList()

    val filteredConversations by remember {
        derivedStateOf {
            conversationList.filter { conv ->
                searchQuery.isBlank() ||
                        conv.customerName.contains(searchQuery, true) ||
                        conv.topic.contains(searchQuery, true) ||
                        conv.lastMessage.contains(searchQuery, true)
            }
        }
    }

    PulseDeskRootBackground {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ==================== PANEL BÊN TRÁI (LEFT SIDEBAR) ====================
            Column(
                modifier = Modifier
                    .width(330.dp)
                    .fillMaxHeight()
                    .glassmorphism(shape = RoundedCornerShape(26.dp), shadowElevation = 18.dp)
                    .padding(18.dp)
            ) {
                // 1. Header (Logo "PulseDesk" & Avatar Online)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PulseDesk",
                            color = TextWhite,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }

                    // Avatar tròn hình người + Chấm xanh "● Online" & Nút Đăng xuất
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onLogOut() }
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Online",
                                    color = NeonGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Nút Đăng xuất nhanh
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x18FFFFFF))
                                .border(0.8.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable { onLogOut() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Log Out",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Search Bar dạng Pill tròn
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color(0x18FFFFFF))
                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(19.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search chats...",
                                    color = TextDim,
                                    fontSize = 12.5.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(color = TextWhite, fontSize = 12.5.sp),
                                cursorBrush = SolidColor(NeonCyan),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Category Tabs ("Active Chats" & "Knowledge Base")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Tab Active Chats
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { currentMainTab = MainContentTab.ACTIVE_CHATS }
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "Active Chats",
                            color = if (currentMainTab == MainContentTab.ACTIVE_CHATS) TextWhite else TextDim,
                            fontSize = 13.sp,
                            fontWeight = if (currentMainTab == MainContentTab.ACTIVE_CHATS) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Gạch chân phát sáng khi tab đang active
                        if (currentMainTab == MainContentTab.ACTIVE_CHATS) {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NeonCyan)
                            )
                        } else {
                            Box(modifier = Modifier.height(2.5.dp))
                        }
                    }

                    // Tab Knowledge Base
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { currentMainTab = MainContentTab.KNOWLEDGE_BASE }
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "Knowledge Base",
                            color = if (currentMainTab == MainContentTab.KNOWLEDGE_BASE) TextWhite else TextDim,
                            fontSize = 13.sp,
                            fontWeight = if (currentMainTab == MainContentTab.KNOWLEDGE_BASE) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (currentMainTab == MainContentTab.KNOWLEDGE_BASE) {
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NeonCyan)
                            )
                        } else {
                            Box(modifier = Modifier.height(2.5.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Danh sách các cuộc hội thoại (LazyColumn)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (currentMainTab == MainContentTab.ACTIVE_CHATS) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = filteredConversations, key = { it.id }) { conv ->
                                ConversationListCard(
                                    conversation = conv,
                                    isSelected = conv.id == selectedConversationId,
                                    onClick = {
                                        selectedConversationId = conv.id
                                        val index = conversationList.indexOfFirst { it.id == conv.id }
                                        if (index != -1 && conversationList[index].unreadCount > 0) {
                                            conversationList[index] = conversationList[index].copy(unreadCount = 0)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        // Knowledge Base Screen
                        KnowledgeBaseScreen(
                            articles = MockData.knowledgeBase,
                            searchQuery = searchQuery,
                            onArticleClick = { selectedKbArticle = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Bottom Navigation Bar nằm trực tiếp trên nền panel (không có khung viền mờ hộp bao quanh)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mục Chats
                    val isChats = currentNavTab == DesktopNavTab.CHATS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { currentNavTab = DesktopNavTab.CHATS }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isChats) Color(0x3500E5BC) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chats",
                                tint = if (isChats) NeonTeal else TextDim,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Chats",
                            color = if (isChats) TextWhite else TextDim,
                            fontSize = 10.5.sp,
                            fontWeight = if (isChats) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Mục AI Assistant
                    val isAi = currentNavTab == DesktopNavTab.AI_ASSISTANT
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { currentNavTab = DesktopNavTab.AI_ASSISTANT }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isAi) Color(0x359D4EDD) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI Assistant",
                                tint = if (isAi) NeonPurple else TextDim,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "AI Assistant",
                            color = if (isAi) TextWhite else TextDim,
                            fontSize = 10.5.sp,
                            fontWeight = if (isAi) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Mục Settings
                    val isSettings = currentNavTab == DesktopNavTab.SETTINGS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { currentNavTab = DesktopNavTab.SETTINGS }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSettings) Color(0x3500F0FF) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isSettings) NeonCyan else TextDim,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Settings",
                            color = if (isSettings) TextWhite else TextDim,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // ==================== PANEL BÊN PHẢI (MAIN CONTENT / CHAT PANE) ====================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (currentNavTab) {
                    DesktopNavTab.CHATS -> {
                        ChatDetailPane(
                            conversation = selectedConversation,
                            messages = activeMessages,
                            isCustomerTyping = isCustomerTyping,
                            autoReplyEnabled = autoReplyEnabled,
                            assignee = userRole,
                            onToggleAutoReply = { autoReplyEnabled = !autoReplyEnabled },
                            onSendMessage = { text, senderType ->
                                handleSendMessage(text, senderType)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    DesktopNavTab.AI_ASSISTANT -> {
                        AiAssistantScreen(modifier = Modifier.fillMaxSize())
                    }
                    DesktopNavTab.SETTINGS -> {
                        SettingsScreen(
                            onSaveSettings = { host, port, _ ->
                                socketClient.connect(host, port.toIntOrNull() ?: 8888)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Hộp thoại xem bài viết tri thức nếu có
        selectedKbArticle?.let { article ->
            KnowledgeDetailDialog(
                article = article,
                onDismiss = { selectedKbArticle = null },
                onApplyToChat = { summary ->
                    handleSendMessage(summary, MessageSenderType.AGENT)
                    currentMainTab = MainContentTab.ACTIVE_CHATS
                }
            )
        }
    }
}
