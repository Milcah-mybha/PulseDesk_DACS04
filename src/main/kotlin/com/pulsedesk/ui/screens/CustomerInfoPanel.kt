package com.pulsedesk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedesk.model.Conversation
import com.pulsedesk.ui.components.glassmorphism
import com.pulsedesk.ui.theme.*

@Composable
fun CustomerInfoPanel(
    conversation: Conversation?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (conversation == null) return

    val gradientColors = AvatarColors[conversation.avatarColorIndex % AvatarColors.size]
    var agentNote by remember(conversation.id) {
        mutableStateOf("Khách hàng SME ưu tiên cao. Đang dùng gói Pro, cần tư vấn thêm tính năng Webhook đa kênh.")
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .glassmorphism(shape = RoundedCornerShape(22.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Tiêu đề & Nút đóng
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "HỒ SƠ KHÁCH HÀNG",
                color = NeonCyan,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Đóng",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Avatar & Tên khách hàng
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, CircleShape, spotColor = gradientColors.first())
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradientColors))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.avatarInitials,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = conversation.customerName,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conversation.isVip) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmberVip.copy(alpha = 0.25f))
                            .border(0.7.dp, AmberVip, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text("KHÁCH VIP", color = AmberVip, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (conversation.isOnline) "Đang Online" else "Ngoại tuyến",
                    color = if (conversation.isOnline) NeonGreen else TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Thống kê nhanh chỉ số CSKH
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoStatCard(title = "Tickets", value = "14", modifier = Modifier.weight(1f))
            InfoStatCard(title = "CSAT", value = "4.9 ⭐", modifier = Modifier.weight(1f))
            InfoStatCard(title = "Cấp độ", value = "Bạc", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Thông tin liên hệ
        Text(
            text = "THÔNG TIN DOANH NGHIỆP",
            color = TextDim,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        InfoRow(icon = Icons.Default.Business, label = "Doanh nghiệp", value = "Công ty TNHH " + conversation.customerName.split(" ").last() + " Tech")
        InfoRow(icon = Icons.Default.Email, label = "Email", value = "contact@" + conversation.customerName.lowercase().replace(" ", "") + ".vn")
        InfoRow(icon = Icons.Default.Phone, label = "Hotline", value = "0988 ••• " + (100..999).random())
        InfoRow(icon = Icons.Default.LocationOn, label = "Khu vực", value = "Hà Nội, Việt Nam")

        Spacer(modifier = Modifier.height(16.dp))

        // Ghi chú nội bộ của Agent
        Text(
            text = "GHI CHÚ NỘI BỘ (CSKH)",
            color = NeonPurple,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x18FFFFFF))
                .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            BasicTextField(
                value = agentNote,
                onValueChange = { agentNote = it },
                textStyle = TextStyle(color = TextWhite, fontSize = 11.5.sp, lineHeight = 16.sp),
                cursorBrush = SolidColor(NeonCyan),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x15FFFFFF))
            .border(0.6.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = TextWhite, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, color = TextDim, fontSize = 9.5.sp)
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, color = TextDim, fontSize = 9.5.sp)
            Text(text = value, color = TextWhite, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}
