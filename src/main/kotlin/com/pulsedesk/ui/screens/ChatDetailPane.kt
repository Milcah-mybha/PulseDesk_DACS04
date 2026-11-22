package com.pulsedesk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedesk.model.ChatMessage
import com.pulsedesk.model.Conversation
import com.pulsedesk.model.MessageSenderType
import com.pulsedesk.ui.components.glassmorphism
import com.pulsedesk.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatDetailPane(
    conversation: Conversation?,
    messages: List<ChatMessage>,
    isCustomerTyping: Boolean = false,
    autoReplyEnabled: Boolean = true,
    showCustomerInfo: Boolean = true,
    onToggleAutoReply: () -> Unit = {},
    onToggleCustomerInfo: () -> Unit = {},
    onSendMessage: (text: String, senderType: MessageSenderType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (conversation == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .glassmorphism(shape = RoundedCornerShape(22.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonCyan.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Chọn một cuộc hội thoại từ danh sách bên trái",
                    color = TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Hệ thống hỗ trợ tự động gợi ý câu trả lời AI & tra cứu chính sách SME",
                    color = TextDim,
                    fontSize = 12.5.sp
                )
            }
        }
        return
    }

    var inputText by remember { mutableStateOf("") }
    var currentSenderRole by remember { mutableStateOf(MessageSenderType.AGENT) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val quickReplies = remember {
        listOf(
            "Em đang kiểm tra log hệ thống cho anh rồi nhé!",
            "Anh vui lòng gửi lại mã giao dịch để em đối soát ngay.",
            "Sự cố đã được bộ phận kỹ thuật khắc phục xong.",
            "Cảm ơn quý khách đã tin tưởng dịch vụ PulseDesk."
        )
    }

    // Tự động cuộn xuống khi có tin nhắn mới hoặc khi khách hàng bắt đầu gõ
    LaunchedEffect(messages.size, isCustomerTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .glassmorphism(shape = RoundedCornerShape(22.dp))
    ) {
        // 1. Chat Header
        ChatHeader(
            conversation = conversation,
            isTyping = isCustomerTyping,
            autoReplyEnabled = autoReplyEnabled,
            showCustomerInfo = showCustomerInfo,
            onToggleAutoReply = onToggleAutoReply,
            onToggleCustomerInfo = onToggleCustomerInfo
        )

        // 2. Chat Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = messages, key = { it.id }) { msg ->
                    ChatBubbleItem(message = msg)
                }

                // Hiệu ứng bong bóng "Khách hàng đang soạn tin..."
                if (isCustomerTyping) {
                    item(key = "typing_indicator") {
                        CustomerTypingBubble(conversation = conversation)
                    }
                }
            }
        }

        // 3. AI Quick Replies Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x229D4EDD))
                    .border(0.8.dp, NeonPurple.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Gợi ý:", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            quickReplies.forEach { reply ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x16FFFFFF))
                        .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                        .clickable {
                            onSendMessage(reply, MessageSenderType.AGENT)
                            scope.launch {
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = reply,
                        color = TextWhite,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // 4. Thanh chọn vai trò người gửi (Cho phép gửi 2 chiều: Tư vấn viên <-> Khách hàng)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Gửi tin với vai trò:",
                    color = TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                // Nút chọn Tư vấn viên
                val isAgent = currentSenderRole == MessageSenderType.AGENT
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAgent) NeonCyan.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                        .border(
                            0.8.dp,
                            if (isAgent) NeonCyan else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { currentSenderRole = MessageSenderType.AGENT }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = if (isAgent) NeonCyan else TextDim,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tư vấn viên",
                            color = if (isAgent) TextWhite else TextDim,
                            fontSize = 11.sp,
                            fontWeight = if (isAgent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Nút chọn Khách hàng (Đóng vai khách hàng để test nhắn qua lại)
                val isCust = currentSenderRole == MessageSenderType.CUSTOMER
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCust) NeonPurple.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                        .border(
                            0.8.dp,
                            if (isCust) NeonPurple else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { currentSenderRole = MessageSenderType.CUSTOMER }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isCust) NeonPurple else TextDim,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${conversation.customerName} (Khách)",
                            color = if (isCust) TextWhite else TextDim,
                            fontSize = 11.sp,
                            fontWeight = if (isCust) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Gợi ý phím tắt
            Text(
                text = "Enter để gửi • Shift+Enter xuống dòng",
                color = TextDim,
                fontSize = 10.5.sp
            )
        }

        // 5. Input Section với phím Enter gửi tin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .glassmorphism(shape = RoundedCornerShape(18.dp), shadowElevation = 8.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (inputText.isEmpty()) {
                        val placeholder = if (currentSenderRole == MessageSenderType.AGENT) {
                            "Nhập phản hồi tư vấn viên..."
                        } else {
                            "Nhập tin nhắn với tư cách khách hàng (${conversation.customerName})..."
                        }
                        Text(
                            text = placeholder,
                            color = TextDim,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(color = TextWhite, fontSize = 13.5.sp),
                        cursorBrush = SolidColor(if (currentSenderRole == MessageSenderType.AGENT) NeonCyan else NeonPurple),
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                    if (keyEvent.isShiftPressed) {
                                        false
                                    } else if (inputText.isNotBlank()) {
                                        onSendMessage(inputText, currentSenderRole)
                                        inputText = ""
                                        true
                                    } else {
                                        true
                                    }
                                } else {
                                    false
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                val sendColor = if (currentSenderRole == MessageSenderType.AGENT) NeonCyan else NeonPurple
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(8.dp, CircleShape, spotColor = sendColor)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) {
                                Brush.linearGradient(listOf(sendColor, Color(0xFF0055FF)))
                            } else {
                                Brush.linearGradient(listOf(Color(0x3000F0FF), Color(0x300055FF)))
                            }
                        )
                        .clickable(enabled = inputText.isNotBlank()) {
                            onSendMessage(inputText, currentSenderRole)
                            inputText = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.White else TextDim,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(
    conversation: Conversation,
    isTyping: Boolean,
    autoReplyEnabled: Boolean,
    showCustomerInfo: Boolean,
    onToggleAutoReply: () -> Unit,
    onToggleCustomerInfo: () -> Unit
) {
    val gradientColors = AvatarColors[conversation.avatarColorIndex % AvatarColors.size]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color(0x18FFFFFF))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradientColors))
                    .border(1.2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.avatarInitials,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.customerName,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (conversation.isVip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberVip.copy(alpha = 0.25f))
                                .border(0.7.dp, AmberVip, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("VIP", color = AmberVip, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.topic,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isTyping) {
                        Text(
                            text = "• đang nhập...",
                            color = NeonCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = if (conversation.isOnline) "• Đang trực tuyến" else "• Ngoại tuyến",
                            color = if (conversation.isOnline) NeonGreen else TextDim,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            // Nút gạt bật/tắt phản hồi khách hàng tự động
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (autoReplyEnabled) NeonCyan.copy(alpha = 0.20f) else Color(0x18FFFFFF))
                    .border(
                        0.8.dp,
                        if (autoReplyEnabled) NeonCyan.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onToggleAutoReply() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (autoReplyEnabled) NeonCyan else TextDim,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (autoReplyEnabled) "Tự động phản hồi: BẬT" else "Tự động phản hồi: TẮT",
                        color = if (autoReplyEnabled) NeonCyan else TextDim,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nút bật/tắt Panel thông tin khách hàng
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (showCustomerInfo) NeonPurple.copy(alpha = 0.25f) else Color(0x18FFFFFF))
                    .border(
                        0.8.dp,
                        if (showCustomerInfo) NeonPurple.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onToggleCustomerInfo() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Hồ sơ",
                        tint = if (showCustomerInfo) NeonPurple else TextDim,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hồ sơ SME",
                        color = if (showCustomerInfo) TextWhite else TextDim,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Bong bóng "Khách hàng đang soạn tin" với 3 chấm nhấp nháy phát sáng
@Composable
private fun CustomerTypingBubble(conversation: Conversation) {
    val gradientColors = AvatarColors[conversation.avatarColorIndex % AvatarColors.size]

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = conversation.avatarInitials,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp), spotColor = Color(0x20000000))
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp))
                .background(Color(0x25FFFFFF))
                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            TypingDotsIndicator(color = NeonCyan)
        }
    }
}

// Animation 3 dấu chấm nhảy mềm mại
@Composable
private fun TypingDotsIndicator(color: Color = NeonCyan) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 180, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        listOf(dot1Scale, dot2Scale, dot3Scale).forEach { scale ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    val isAgent = message.senderType == MessageSenderType.AGENT
    val isSystem = message.senderType == MessageSenderType.SYSTEM

    if (isSystem) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(0.6.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = message.message,
                    color = TextDim,
                    fontSize = 11.sp
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAgent) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .then(
                    if (isAgent) {
                        Modifier
                            .shadow(8.dp, RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp), spotColor = NeonCyan)
                            .clip(RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0x4500F0FF), Color(0x350055FF)))
                            )
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.8f), Color(0x30FFFFFF))),
                                RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp)
                            )
                    } else {
                        Modifier
                            .shadow(6.dp, RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp), spotColor = Color(0x20000000))
                            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp))
                    }
                )
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text = message.message,
                color = TextWhite,
                fontSize = 13.5.sp,
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = message.timestamp,
                color = TextDim,
                fontSize = 10.5.sp
            )
            if (isAgent) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
