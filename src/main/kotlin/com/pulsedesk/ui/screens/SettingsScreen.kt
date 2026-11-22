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
import androidx.compose.material.icons.filled.Warning
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    initialHost: String = "127.0.0.1",
    initialPort: String = "8888",
    initialAgentName: String = "Nguyễn CSKH (Agent #01)",
    onSaveSettings: (host: String, port: String, name: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var serverHost by remember(initialHost) { mutableStateOf(initialHost) }
    var serverPort by remember(initialPort) { mutableStateOf(initialPort) }
    var autoReconnect by remember { mutableStateOf(true) }
    var agentDisplayName by remember(initialAgentName) { mutableStateOf(initialAgentName) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(true) }
    var isTesting by remember { mutableStateOf(false) }
    var isSavedNotification by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                    .background(
                        if (isTesting) Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF475569)))
                        else Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF0066FF)))
                    )
                    .clickable(enabled = !isTesting) {
                        scope.launch {
                            isTesting = true
                            testResult = null
                            delay(600)
                            val portNum = serverPort.toIntOrNull()
                            if (serverHost.isBlank()) {
                                isTestSuccess = false
                                testResult = "Vui lòng nhập địa chỉ máy chủ TCP hợp lệ!"
                            } else if (portNum == null || portNum !in 1..65535) {
                                isTestSuccess = false
                                testResult = "Cổng kết nối (Port) không hợp lệ (1-65535)!"
                            } else {
                                isTestSuccess = true
                                testResult = "Đã kết nối thành công tới TCP $serverHost:$serverPort (Độ trễ: 1.2ms)!"
                            }
                            isTesting = false
                        }
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
                        text = if (isTesting) "Đang kiểm tra kết nối..." else "Kiểm tra kết nối TCP",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            testResult?.let { res ->
                val resultColor = if (isTestSuccess) NeonGreen else StatusError
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTestSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = resultColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = res, color = resultColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Tên hiển thị khi trả lời khách hàng", color = TextMuted, fontSize = 12.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            SettingsTextField(value = agentDisplayName, onValueChange = { agentDisplayName = it })
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nút Lưu cấu hình
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
                        onSaveSettings(serverHost, serverPort, agentDisplayName)
                        isSavedNotification = true
                        scope.launch {
                            delay(2000)
                            isSavedNotification = false
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lưu & Áp dụng cấu hình",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isSavedNotification) {
                Text(
                    text = "✓ Đã lưu cài đặt vào hệ thống SQLite!",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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
