package com.pulsedesk.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedesk.app.model.ConversationCategory
import com.pulsedesk.app.network.SocketConnectionState
import com.pulsedesk.app.ui.theme.*

// Modifier tạo hiệu ứng kính mờ (Glassmorphism) với viền bắt sáng và đổ bóng mềm
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = GlassSurface,
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 10.dp,
    shadowColor: Color = GlassShadow
): Modifier = this
    .shadow(
        elevation = shadowElevation,
        shape = shape,
        clip = false,
        ambientColor = Color(0x40000000),
        spotColor = shadowColor
    )
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                backgroundColor.copy(alpha = 0.22f),
                backgroundColor.copy(alpha = 0.06f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                GlassHighlight,
                Color(0x15FFFFFF),
                NeonCyan.copy(alpha = 0.25f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        ),
        shape = shape
    )

@Composable
fun PulseDeskRootBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(BgDarkStart, BgDarkMid1, BgDarkMid2, BgDarkEnd)
                )
            )
    ) {
        // Vùng sáng tím phát xạ
        Canvas(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-60).dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.40f), Color(0x157209B7), Color.Transparent),
                    radius = size.width / 2
                )
            )
        }

        // Vùng sáng Cyan phát xạ
        Canvas(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-160).dp, y = 100.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.35f), Color(0x1000B4D8), Color.Transparent),
                    radius = size.width / 2
                )
            )
        }

        // Vùng sáng hồng phát xạ
        Canvas(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 120.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPink.copy(alpha = 0.30f), Color(0x10FF007F), Color.Transparent),
                    radius = size.width / 2
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
        ) {
            content()
        }
    }
}

@Composable
fun GlassmorphismTopAppBar(
    socketState: SocketConnectionState,
    onAvatarClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .glassmorphism(shape = RoundedCornerShape(26.dp), shadowElevation = 14.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Biểu tượng Pulse phát sáng
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(8.dp, CircleShape, spotColor = NeonCyan)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonCyan, Color(0xFF0066FF)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("P", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PulseDesk",
                            color = TextWhite,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = " Hub",
                            color = NeonCyan,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Trạng thái Socket 8888
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val isConnected = socketState == SocketConnectionState.CONNECTED
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isConnected) NeonGreen else Color(0xFFFF9E00),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isConnected) "TCP :8888 Trực tuyến" else "Đang kết nối lại :8888",
                            color = if (isConnected) NeonGreen else Color(0xFFFF9E00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Avatar Hỗ trợ viên với chấm Online nhịp thở
            GlowingUserAvatar(initials = "CS", onClick = onAvatarClick)
        }
    }
}

@Composable
fun GlowingUserAvatar(
    initials: String,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val haloScale by transition.animateFloat(
        initialValue = 1.0f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "scale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.7f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED))))
                .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Hào quang xanh nhấp nháy
        Box(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer { scaleX = haloScale; scaleY = haloScale; alpha = haloAlpha }
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = 0.5f))
        )
        // Chấm online
        Box(
            modifier = Modifier
                .size(11.dp)
                .align(Alignment.BottomEnd)
                .shadow(6.dp, CircleShape, spotColor = NeonGreen)
                .clip(CircleShape)
                .background(NeonGreen)
                .border(2.dp, Color(0xFF04060E), CircleShape)
        )
    }
}

@Composable
fun GlassmorphicSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassmorphism(shape = RoundedCornerShape(20.dp), shadowElevation = 8.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Tìm kiếm hội thoại, khách hàng...", color = TextDim, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    textStyle = TextStyle(color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(NeonCyan),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    tint = TextDim,
                    modifier = Modifier.size(18.dp).clickable { onQueryChanged("") }
                )
            }
        }
    }
}

@Composable
fun GlassmorphicFilterTabs(
    selectedCategory: ConversationCategory,
    onCategorySelected: (ConversationCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ConversationCategory.values().forEach { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF0072FF))))
                                .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        }
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.displayName,
                    color = if (isSelected) Color.White else TextMuted,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// Huy hiệu tin chưa đọc phát sáng rực rỡ
@Composable
fun GlowingUnreadBadge(count: Int) {
    if (count <= 0) return
    Box(
        modifier = Modifier
            .shadow(12.dp, CircleShape, spotColor = NeonCyan)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(NeonCyan, Color(0xFF0055FF))))
            .border(1.2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun GlassmorphicBottomBar(
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 14.dp)
            .glassmorphism(shape = RoundedCornerShape(28.dp), shadowElevation = 18.dp, shadowColor = Color(0x3500F0FF))
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Default.ChatBubble, "Hội thoại", selectedTab == 0) { onTabSelected(0) }
            BottomNavItem(Icons.Default.AutoAwesome, "AI Assistant", selectedTab == 1) { onTabSelected(1) }
            BottomNavItem(Icons.Default.Settings, "Cài đặt", selectedTab == 2) { onTabSelected(2) }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(
                    if (isSelected) {
                        Modifier
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.22f))
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) NeonCyan else TextDim,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = if (isSelected) NeonCyan else TextDim,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
