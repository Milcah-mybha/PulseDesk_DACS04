package com.pulsedesk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.pulsedesk.model.Conversation
import com.pulsedesk.ui.components.GlowingUnreadBadge
import com.pulsedesk.ui.theme.*

// Thẻ hội thoại kính mờ dạng cuộn dọc (LazyColumn)
@Composable
fun ConversationListCard(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = AvatarColors[conversation.avatarColorIndex % AvatarColors.size]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 14.dp else (if (conversation.unreadCount > 0) 8.dp else 4.dp),
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isSelected) NeonCyan else (if (conversation.unreadCount > 0) Color(0x3500F0FF) else Color(0x20000000))
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    if (isSelected) {
                        listOf(Color(0x3500F0FF), Color(0x200044FF))
                    } else {
                        listOf(GlassSurface.copy(alpha = 0.28f), GlassSurface.copy(alpha = 0.08f))
                    }
                )
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                brush = Brush.linearGradient(
                    if (isSelected) {
                        listOf(NeonCyan, Color.White.copy(alpha = 0.8f))
                    } else {
                        listOf(GlassHighlight, Color(0x15FFFFFF), NeonCyan.copy(alpha = 0.25f))
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar khách hàng với viền phát sáng & chấm online
            Box(modifier = Modifier.size(46.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradientColors))
                        .border(1.2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
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
                            .border(2.dp, Color(0xFF070B14), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Thông tin: Tên, Chủ đề/Ticket, Tin nhắn mới nhất
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.customerName,
                        color = TextWhite,
                        fontSize = 14.5.sp,
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
                                .background(AmberVip.copy(alpha = 0.22f))
                                .border(0.7.dp, AmberVip.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AmberVip,
                                    modifier = Modifier.size(9.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "VIP",
                                    color = AmberVip,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Chủ đề / Ticket tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x18FFFFFF))
                        .border(0.5.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = conversation.topic,
                        color = NeonCyan,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = conversation.lastMessage,
                    color = if (conversation.unreadCount > 0) Color(0xFFF1F5F9) else TextDim,
                    fontSize = 12.5.sp,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Thời gian & Huy hiệu tin chưa đọc phát sáng
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = conversation.timestamp,
                    color = if (conversation.unreadCount > 0) NeonCyan else TextDim,
                    fontSize = 11.sp,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(6.dp))

                GlowingUnreadBadge(count = conversation.unreadCount)
            }
        }
    }
}
