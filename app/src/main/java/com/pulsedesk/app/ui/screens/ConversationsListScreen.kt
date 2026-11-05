package com.pulsedesk.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pulsedesk.app.model.Conversation
import com.pulsedesk.app.model.ConversationCategory
import com.pulsedesk.app.ui.components.GlassmorphicBottomBar
import com.pulsedesk.app.ui.components.GlassmorphicFilterTabs
import com.pulsedesk.app.ui.components.GlassmorphicSearchBar
import com.pulsedesk.app.ui.components.GlassmorphismTopAppBar
import com.pulsedesk.app.ui.components.GlowingUnreadBadge
import com.pulsedesk.app.ui.components.PulseDeskRootBackground
import com.pulsedesk.app.ui.theme.AmberVip
import com.pulsedesk.app.ui.theme.AvatarColors
import com.pulsedesk.app.ui.theme.GlassHighlight
import com.pulsedesk.app.ui.theme.GlassSurface
import com.pulsedesk.app.ui.theme.NeonCyan
import com.pulsedesk.app.ui.theme.NeonGreen
import com.pulsedesk.app.ui.theme.NeonPurple
import com.pulsedesk.app.ui.theme.TextDim
import com.pulsedesk.app.ui.theme.TextMuted
import com.pulsedesk.app.ui.theme.TextWhite
import com.pulsedesk.app.viewmodel.ConversationsUiState
import com.pulsedesk.app.viewmodel.ConversationsViewModel

@Composable
fun ConversationsListScreen(
    viewModel: ConversationsViewModel = viewModel(),
    onConversationClick: (Conversation) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    ConversationsListContent(
        uiState = uiState,
        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
        onCategorySelected = { viewModel.selectCategory(it) },
        onConversationClick = onConversationClick,
        onSimulateSocketMessage = { viewModel.simulateIncomingSocketMessage() }
    )
}

@Composable
fun ConversationsListContent(
    uiState: ConversationsUiState,
    onSearchQueryChanged: (String) -> Unit = {},
    onCategorySelected: (ConversationCategory) -> Unit = {},
    onConversationClick: (Conversation) -> Unit = {},
    onSimulateSocketMessage: () -> Unit = {}
) {
    var currentBottomTab by remember { mutableStateOf(0) }

    PulseDeskRootBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    GlassmorphismTopAppBar(
                        socketState = uiState.socketState
                    )

                    GlassmorphicSearchBar(
                        query = uiState.searchQuery,
                        onQueryChanged = onSearchQueryChanged
                    )

                    GlassmorphicFilterTabs(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = onCategorySelected
                    )

                    AnimatedVisibility(
                        visible = uiState.lastReceivedEventMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        uiState.lastReceivedEventMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x2500F0FF))
                                    .border(0.8.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg,
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                GlassmorphicBottomBar(
                    selectedTab = currentBottomTab,
                    onTabSelected = { currentBottomTab = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.filteredConversations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy cuộc hội thoại phù hợp.",
                            color = TextDim,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 90.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "simulate_socket_banner") {
                            SimulateSocketBanner(onSimulate = onSimulateSocketMessage)
                        }

                        items(
                            items = uiState.filteredConversations,
                            key = { it.id }
                        ) { conversation ->
                            ConversationCardItem(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationCardItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val gradientColors = AvatarColors[
        conversation.avatarColorIndex % AvatarColors.size
    ]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (conversation.unreadCount > 0) 12.dp else 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (conversation.unreadCount > 0) Color(0x3500F0FF) else Color(0x18000000)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(GlassSurface.copy(alpha = 0.22f), GlassSurface.copy(alpha = 0.06f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(GlassHighlight, Color(0x15FFFFFF), NeonCyan.copy(alpha = 0.25f))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
                Box(modifier = Modifier.size(54.dp)) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(gradientColors))
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.avatarInitials,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (conversation.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .shadow(4.dp, CircleShape, spotColor = NeonGreen)
                                .clip(CircleShape)
                                .background(NeonGreen)
                                .border(2.5.dp, Color(0xFF070B14), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = conversation.customerName,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (conversation.isVip) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AmberVip.copy(alpha = 0.2f))
                                    .border(0.8.dp, AmberVip.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = AmberVip,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "VIP",
                                        color = AmberVip,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18FFFFFF))
                            .border(0.5.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = conversation.topic,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = conversation.lastMessage,
                        color = if (conversation.unreadCount > 0) Color(0xFFF1F5F9) else TextMuted,
                        fontSize = 13.5.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = conversation.timestamp,
                        color = if (conversation.unreadCount > 0) NeonCyan else TextDim,
                        fontSize = 11.5.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GlowingUnreadBadge(count = conversation.unreadCount)
                }
            }
        }
}

@Composable
private fun SimulateSocketBanner(onSimulate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0x309D4EDD), Color(0x3000F0FF))))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(NeonPurple.copy(alpha = 0.7f), NeonCyan.copy(alpha = 0.7f))),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSimulate)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Kiểm thử Socket thời gian thực", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Chạm để gửi tin nhắn mô phỏng & nhảy số badge", color = TextDim, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonCyan.copy(alpha = 0.25f))
                    .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Gửi gói tin", color = NeonCyan, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
