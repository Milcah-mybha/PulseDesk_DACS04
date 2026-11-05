package com.pulsedesk.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedesk.app.model.ChatMessage
import com.pulsedesk.app.model.Conversation
import com.pulsedesk.app.model.MessageSenderType
import com.pulsedesk.app.ui.components.PulseDeskRootBackground
import com.pulsedesk.app.ui.components.glassmorphism
import com.pulsedesk.app.ui.theme.AmberVip
import com.pulsedesk.app.ui.theme.AvatarColors
import com.pulsedesk.app.ui.theme.NeonCyan
import com.pulsedesk.app.ui.theme.NeonGreen
import com.pulsedesk.app.ui.theme.NeonPurple
import com.pulsedesk.app.ui.theme.TextDim
import com.pulsedesk.app.ui.theme.TextWhite
import com.pulsedesk.app.viewmodel.ChatDetailViewModel

@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    viewModel: ChatDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(conversation.id) {
        viewModel.loadConversation(conversation)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    PulseDeskRootBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChatDetailTopBar(
                    conversation = conversation,
                    onBackClick = onBackClick
                )
            },
            bottomBar = {
                ChatDetailInputSection(
                    input = uiState.inputMessage,
                    quickReplies = uiState.quickReplies,
                    onInputChanged = { viewModel.onInputMessageChanged(it) },
                    onSend = { viewModel.sendMessage() },
                    onQuickReplyClick = { viewModel.sendMessage(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Đang kết nối phiên tư vấn cùng ${conversation.customerName}...",
                                    color = TextDim,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    } else {
                        items(items = uiState.messages, key = { it.id }) { message ->
                            ChatMessageBubble(message = message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatDetailTopBar(
    conversation: Conversation,
    onBackClick: () -> Unit
) {
    val gradientColors = AvatarColors[conversation.avatarColorIndex % AvatarColors.size]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .glassmorphism(shape = RoundedCornerShape(26.dp), shadowElevation = 14.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradientColors))
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.avatarInitials,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (conversation.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .shadow(4.dp, CircleShape, spotColor = NeonGreen)
                            .clip(CircleShape)
                            .background(NeonGreen)
                            .border(2.dp, Color(0xFF04060E), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.customerName,
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.isVip) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberVip.copy(alpha = 0.25f))
                                .border(0.8.dp, AmberVip, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AmberVip, modifier = Modifier.size(9.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("VIP", color = AmberVip, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x18FFFFFF))
                            .border(0.5.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = conversation.topic,
                            color = NeonCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (conversation.isOnline) "Đang trực tuyến" else "Ngoại tuyến",
                        color = if (conversation.isOnline) NeonGreen else TextDim,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isAgent = message.senderType == MessageSenderType.AGENT

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAgent) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (isAgent) {
                        Modifier
                            .shadow(8.dp, RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp), spotColor = NeonCyan)
                            .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0x4000F0FF), Color(0x350055FF))
                                )
                            )
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.8f), Color(0x30FFFFFF))),
                                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                            )
                    } else {
                        Modifier
                            .shadow(6.dp, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp), spotColor = Color(0x20000000))
                            .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                            .background(Color(0x1EFFFFFF))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                            )
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.message,
                color = TextWhite,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

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
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatDetailInputSection(
    input: String,
    quickReplies: List<String>,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onQuickReplyClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        if (quickReplies.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x229D4EDD))
                        .border(0.8.dp, NeonPurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI:", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                quickReplies.forEach { reply ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x18FFFFFF))
                            .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { onQuickReplyClick(reply) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = reply,
                            color = TextWhite,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(shape = RoundedCornerShape(26.dp), shadowElevation = 12.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (input.isEmpty()) {
                        Text("Nhập phản hồi tư vấn...", color = TextDim, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChanged,
                        textStyle = TextStyle(color = TextWhite, fontSize = 14.5.sp),
                        cursorBrush = SolidColor(NeonCyan),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(8.dp, CircleShape, spotColor = NeonCyan)
                        .clip(CircleShape)
                        .background(
                            if (input.isNotBlank()) {
                                Brush.linearGradient(listOf(NeonCyan, Color(0xFF0066FF)))
                            } else {
                                Brush.linearGradient(listOf(Color(0x3300F0FF), Color(0x330066FF)))
                            }
                        )
                        .clickable(enabled = input.isNotBlank(), onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (input.isNotBlank()) Color.White else TextDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
