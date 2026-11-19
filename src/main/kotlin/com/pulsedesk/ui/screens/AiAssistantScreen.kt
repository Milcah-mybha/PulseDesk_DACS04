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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedesk.data.MockData
import com.pulsedesk.model.AiAssistantMessage
import com.pulsedesk.model.RagCitation
import com.pulsedesk.ui.components.glassmorphism
import com.pulsedesk.ui.theme.*

@Composable
fun AiAssistantScreen(
    modifier: Modifier = Modifier
) {
    var messages by remember { mutableStateOf(MockData.aiAssistantInitialMessages) }
    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestedQuestions = listOf(
        "Quy trình xử lý sự cố lỗi kết nối 502?",
        "Chính sách chiết khấu hợp đồng gói Enterprise?",
        "Yêu cầu bảo mật xác thực 2 bước (2FA) nhân viên?"
    )

    fun handleSend(query: String) {
        if (query.isBlank()) return
        val userMsg = AiAssistantMessage(
            id = "user_${java.util.UUID.randomUUID()}",
            isUser = true,
            text = query,
            timestamp = "Vừa xong"
        )
        val botMsg = AiAssistantMessage(
            id = "bot_${java.util.UUID.randomUUID()}",
            isUser = false,
            text = "Dựa trên tài liệu hệ thống nội bộ SME PulseDesk, câu hỏi: \"$query\" đã được đối soát qua RAG Engine.\n\nThông tin tóm lược: Hệ thống CSKH áp dụng quy trình xử lý ticket cấp 1 trong 15 phút và tự động phân luồng qua hàng đợi dự phòng khi phát hiện sự cố cổng thanh toán hoặc hóa đơn điện tử.",
            timestamp = "Vừa xong",
            citations = listOf(
                RagCitation("SOP-CSKH: Quy trình xử lý yêu cầu khách hàng SME", "Mục 2.1: Phân loại ticket khẩn cấp", 0.95f),
                RagCitation("KB-04: Cấu hình tích hợp Webhook Socket", "Phần 4.2", 0.89f)
            )
        )
        messages = messages + userMsg + botMsg
        inputQuery = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .glassmorphism(shape = RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // Tiêu đề AI Assistant
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(8.dp, CircleShape, spotColor = NeonPurple)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, Color(0xFF6B21A8)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PulseDesk AI Assistant",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonPurple.copy(alpha = 0.25f))
                            .border(0.8.dp, NeonPurple, RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("RAG Engine Active", color = NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Hỏi đáp tra cứu cơ sở tri thức nội bộ & chính sách SME có trích dẫn nguồn tham khảo",
                    color = TextDim,
                    fontSize = 11.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Danh sách tin nhắn hỏi đáp
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items = messages, key = { it.id }) { msg ->
                AiMessageBubble(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Câu hỏi gợi ý nhanh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedQuestions.forEach { question ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { handleSend(question) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = question,
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Khung nhập câu hỏi
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(shape = RoundedCornerShape(18.dp), shadowElevation = 8.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (inputQuery.isEmpty()) {
                        Text(
                            text = "Hỏi AI Copilot về tài liệu, quy trình hoặc sự cố (Enter để gửi)...",
                            color = TextDim,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        textStyle = TextStyle(color = TextWhite, fontSize = 13.5.sp),
                        cursorBrush = SolidColor(NeonPurple),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                    handleSend(inputQuery)
                                    true
                                } else false
                            }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(8.dp, CircleShape, spotColor = NeonPurple)
                        .clip(CircleShape)
                        .background(
                            if (inputQuery.isNotBlank()) {
                                Brush.linearGradient(listOf(NeonPurple, Color(0xFF6B21A8)))
                            } else {
                                Brush.linearGradient(listOf(Color(0x309D4EDD), Color(0x306B21A8)))
                            }
                        )
                        .clickable(enabled = inputQuery.isNotBlank()) { handleSend(inputQuery) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputQuery.isNotBlank()) Color.White else TextDim,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(message: AiAssistantMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .then(
                    if (message.isUser) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp))
                            .background(Brush.linearGradient(listOf(Color(0x4000F0FF), Color(0x300055FF))))
                            .border(1.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp))
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp))
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp))
                    }
                )
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = TextWhite,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )

                // Trích dẫn nguồn tham khảo RAG (Citations)
                if (message.citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nguồn tham khảo đối soát:",
                        color = NeonTeal,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.citations.forEach { citation ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1800E5BC))
                                    .border(0.6.dp, NeonTeal.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = NeonTeal,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = citation.title,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = citation.section,
                                                color = TextDim,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${(citation.score * 100).toInt()}% match",
                                        color = NeonTeal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = message.timestamp,
            color = TextDim,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
