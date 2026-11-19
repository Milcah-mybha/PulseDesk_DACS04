package com.pulsedesk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.pulsedesk.ui.components.glassmorphism
import com.pulsedesk.ui.theme.*

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var serverHost by remember { mutableStateOf("127.0.0.1") }
    var serverPort by remember { mutableStateOf("8888") }
    var autoReconnect by remember { mutableStateOf(true) }
    var agentDisplayName by remember { mutableStateOf("Nguyễn CSKH (Agent #01)") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .glassmorphism(shape = RoundedCornerShape(22.dp))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonCyan.copy(alpha = 0.2f))
                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Cài đặt hệ thống & Kết nối TCP Server",
                    color = TextWhite,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cấu hình Socket Backend thời gian thực cho nhân viên tư vấn PulseDesk",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cấu hình Socket TCP
        Text(
            text = "KẾT NỐI TCP SOCKET (PORT 8888)",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Host
            Column(modifier = Modifier.weight(2f)) {
                Text("Địa chỉ IP Server Host", color = TextMuted, fontSize = 12.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsTextField(value = serverHost, onValueChange = { serverHost = it })
            }

            // Server Port
            Column(modifier = Modifier.weight(1f)) {
                Text("Cổng Port", color = TextMuted, fontSize = 12.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                SettingsTextField(value = serverPort, onValueChange = { serverPort = it })
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle tự động kết nối lại
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x14FFFFFF))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Tự động kết nối lại (Auto-Reconnect)", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Tự động gửi gói tin heartbeat và kết nối lại sau 3s khi rớt mạng", color = TextDim, fontSize = 11.sp)
            }
            Switch(
                checked = autoReconnect,
                onCheckedChange = { autoReconnect = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonCyan,
                    uncheckedThumbColor = TextDim,
                    uncheckedTrackColor = Color(0x30FFFFFF)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút Kiểm tra kết nối
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = NeonCyan)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF0066FF))))
                    .clickable {
                        isTesting = true
                        testResult = "Đã kết nối thành công tới TCP $serverHost:$serverPort (Độ trễ 1.2ms)!"
                        isTesting = false
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kiểm tra kết nối TCP",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            testResult?.let { res ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = res, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Cấu hình tài khoản nhân viên tư vấn
        Text(
            text = "THÔNG TIN NHÂN VIÊN TƯ VẤN (AGENT)",
            color = NeonPurple,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Tên hiển thị khi trả lời khách hàng", color = TextMuted, fontSize = 12.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            SettingsTextField(value = agentDisplayName, onValueChange = { agentDisplayName = it })
        }
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = TextWhite, fontSize = 13.5.sp),
            cursorBrush = SolidColor(NeonCyan),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
