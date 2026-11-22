package com.pulsedesk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedesk.model.ChatMessage
import com.pulsedesk.model.Conversation
import com.pulsedesk.model.FileAttachment
import com.pulsedesk.model.MessageSenderType
import com.pulsedesk.ui.components.glassmorphism
import com.pulsedesk.ui.theme.*

@Composable
fun ChatDetailPane(
    conversation: Conversation?,
    messages: List<ChatMessage>,
    isCustomerTyping: Boolean = false,
    autoReplyEnabled: Boolean = true,
    showCustomerInfo: Boolean = true,
    assignee: String = "Staff Member",
    onToggleAutoReply: () -> Unit = {},
    onToggleCustomerInfo: () -> Unit = {},
    onSendMessage: (text: String, senderType: MessageSenderType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (conversation == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .glassmorphism(shape = RoundedCornerShape(26.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = TextDim,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Select a conversation from the left to start chatting",
                    color = TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val aiSuggestion = "Ask for order confirmation ID"

    LaunchedEffect(messages.size, isCustomerTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .glassmorphism(shape = RoundedCornerShape(26.dp), shadowElevation = 16.dp)
    ) {
        // 1. Chat Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tên cuộc hội thoại & Trạng thái Active
                Column {
                    Text(
                        text = "Chat with ${conversation.customerName}",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Status: ",
                            color = TextDim,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "Active",
                            color = NeonTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Badge Assignee: Staff Member
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x18FFFFFF))
                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Assignee: $assignee",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Đường kẻ phân cách mờ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x15FFFFFF))
        )

        // 2. Danh sách tin nhắn cuộn mượt
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items = messages, key = { it.id }) { msg ->
                    if (msg.fileAttachment != null) {
                        DocumentAttachmentItem(attachment = msg.fileAttachment)
                    } else if (msg.message.isNotBlank()) {
                        ChatBubbleRow(message = msg)
                    }
                }
            }
        }

        // 3. AI Suggestion Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x209D4EDD))
                    .border(0.8.dp, NeonPurple.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable {
                        inputText = "Could you please provide your order confirmation ID?"
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🪄 AI Suggestion: ",
                        color = NeonPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = aiSuggestion,
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. Input Bar (Nút Attach + Khung nhập + Micro + Nút Send)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Nút Attach file 📎
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x1CFFFFFF))
                    .border(0.8.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable {
                        // Giả lập gửi file đính kèm
                        onSendMessage("Here is the PDF of the return receipt...", MessageSenderType.CUSTOMER)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Khung nhập tin nhắn dạng Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x18FFFFFF))
                    .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Type your message here...",
                                color = TextDim,
                                fontSize = 13.5.sp
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(color = TextWhite, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(NeonCyan),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                        if (inputText.isNotBlank()) {
                                            onSendMessage(inputText, MessageSenderType.AGENT)
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

                    // Icon Microphone 🎙️
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = TextDim,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { }
                    )
                }
            }

            // Nút Send hình viên thuốc phát sáng
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = NeonTeal)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00B4D8), Color(0xFF0077B6))
                        )
                    )
                    .border(1.dp, NeonTeal.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                    .clickable {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText, MessageSenderType.AGENT)
                            inputText = ""
                        }
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Send",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// Bong bóng tin nhắn văn bản
@Composable
private fun ChatBubbleRow(message: ChatMessage) {
    val isAgent = message.senderType == MessageSenderType.AGENT

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAgent) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar icon người cho tin nhắn khách hàng bên trái
        if (!isAgent) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .border(0.8.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .then(
                    if (isAgent) {
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x35101E3C))
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.7f), Color(0x30FFFFFF))),
                                RoundedCornerShape(20.dp)
                            )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x24FFFFFF))
                            .border(
                                0.8.dp,
                                Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(20.dp)
                            )
                    }
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.message,
                    color = TextWhite,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )

                // Chấm xanh ngọc phát sáng ở tin nhắn của Agent
                if (isAgent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NeonTeal)
                    )
                }
            }
        }
    }
}

// Thẻ tài liệu đính kèm (File PDF return_receipt.pdf với SHA-256 Verified)
@Composable
private fun DocumentAttachmentItem(attachment: FileAttachment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar icon người bên trái
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
                .border(0.8.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Khung tài liệu PDF
        Box(
            modifier = Modifier
                .widthIn(min = 340.dp, max = 460.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x26FFFFFF))
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(Color(0x50FFFFFF), NeonCyan.copy(alpha = 0.4f))),
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon file chữ nhật bo góc
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x30FFFFFF))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "PDF",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = attachment.fileName,
                        color = TextWhite,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Badge SHA-256 Verified
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2500E5BC))
                                .border(0.6.dp, NeonTeal.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonTeal,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SHA-256 Verified",
                                    color = NeonTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Hash code
                        Text(
                            text = attachment.verificationHash,
                            color = TextDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
