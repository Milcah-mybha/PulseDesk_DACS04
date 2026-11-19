package com.pulsedesk.ui.screens

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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Star
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
    onSendMessage: (String) -> Unit,
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val quickReplies = remember(conversation.id) {
        listOf(
            "Em đang kiểm tra log hệ thống cho anh rồi nhé!",
            "Anh vui lòng gửi lại mã giao dịch để em đối soát ngay.",
            "Sự cố đã được bộ phận kỹ thuật khắc phục xong.",
            "Cảm ơn quý khách đã tin tưởng dịch vụ PulseDesk."
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .glassmorphism(shape = RoundedCornerShape(22.dp))
    ) {
        // 1. Chat Header
        ChatHeader(conversation = conversation)

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
                            onSendMessage(reply)
                            scope.launch {
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
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

        // 4. Input Section với phím Enter gửi tin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .glassmorphism(shape = RoundedCornerShape(18.dp), shadowElevation = 8.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Nhập phản hồi tư vấn (Nhấn Enter để gửi, Shift+Enter xuống dòng)...",
                            color = TextDim,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(color = TextWhite, fontSize = 13.5.sp),
                        cursorBrush = SolidColor(NeonCyan),
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                    if (keyEvent.isShiftPressed) {
                                        false
                                    } else {
                                        if (inputText.isNotBlank()) {
                                            onSendMessage(inputText)
                                            inputText = ""
                                        }
                                        true
                                    }
                                } else {
                                    false
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(8.dp, CircleShape, spotColor = NeonCyan)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) {
                                Brush.linearGradient(listOf(NeonCyan, Color(0xFF0055FF)))
                            } else {
                                Brush.linearGradient(listOf(Color(0x3000F0FF), Color(0x300055FF)))
                            }
                        )
                        .clickable(enabled = inputText.isNotBlank()) {
                            onSendMessage(inputText)
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
private fun ChatHeader(conversation: Conversation) {
    val gradientColors = AvatarColors[conversation.avatarColorIndex % AvatarColors.size]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.dp,
                color = Color.Transparent,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
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
                    Text(
                        text = if (conversation.isOnline) "• Đang trực tuyến" else "• Ngoại tuyến",
                        color = if (conversation.isOnline) NeonGreen else TextDim,
                        fontSize = 10.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    val isAgent = message.senderType == MessageSenderType.AGENT

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
                            .background(Color(0x20FFFFFF))
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
