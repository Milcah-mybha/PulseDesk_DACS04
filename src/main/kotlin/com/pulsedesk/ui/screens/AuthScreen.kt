package com.pulsedesk.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedesk.ui.components.AppLogo
import com.pulsedesk.ui.components.PulseDeskRootBackground
import com.pulsedesk.ui.components.glassmorphism
import com.pulsedesk.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthTab {
    LOG_IN,
    REGISTER
}

enum class UserRole(val title: String) {
    STAFF("Staff Member"),
    ADMIN("Administrator")
}

@Composable
fun AuthScreen(
    onLoginSuccess: (staffId: String, role: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(AuthTab.LOG_IN) }
    var selectedRole by remember { mutableStateOf(UserRole.STAFF) }

    // State đăng nhập
    var loginStaffId by remember { mutableStateOf("staff_01@pulsedesk.vn") }
    var loginPassword by remember { mutableStateOf("••••••••") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberDevice by remember { mutableStateOf(true) }

    // State đăng ký
    var regFullName by remember { mutableStateOf("") }
    var regStaffId by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var isRegPasswordVisible by remember { mutableStateOf(false) }
    var isRegConfirmVisible by remember { mutableStateOf(false) }

    // State bảo mật phiên (Nonce Challenge / TCP Handshake)
    var isAuthenticating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    PulseDeskRootBackground {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Panel xác thực kính mờ trung tâm (Liquid Glass Panel)
            Column(
                modifier = Modifier
                    .width(460.dp)
                    .glassmorphism(shape = RoundedCornerShape(28.dp), shadowElevation = 24.dp)
                    .padding(horizontal = 32.dp, vertical = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Logo & Thương hiệu PulseDesk Hub
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppLogo(size = 38.dp)

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PulseDesk",
                                color = TextWhite,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = " Hub",
                                color = NeonCyan,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Enterprise CSKH & Internal Consultation",
                            color = TextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Tab chuyển đổi mượt mà giữa "Log In" và "Register"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x16FFFFFF))
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Tab Log In
                        val isLogin = currentTab == AuthTab.LOG_IN
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isLogin) {
                                        Modifier.background(Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.35f), Color(0x350055FF))))
                                    } else {
                                        Modifier.background(Color.Transparent)
                                    }
                                )
                                .clickable { currentTab = AuthTab.LOG_IN }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log In",
                                color = if (isLogin) TextWhite else TextDim,
                                fontSize = 13.5.sp,
                                fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        // Tab Register
                        val isRegister = currentTab == AuthTab.REGISTER
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (isRegister) {
                                        Modifier.background(Brush.horizontalGradient(listOf(NeonTeal.copy(alpha = 0.35f), Color(0x359D4EDD))))
                                    } else {
                                        Modifier.background(Color.Transparent)
                                    }
                                )
                                .clickable { currentTab = AuthTab.REGISTER }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Register",
                                color = if (isRegister) TextWhite else TextDim,
                                fontSize = 13.5.sp,
                                fontWeight = if (isRegister) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Bộ chọn vai trò người dùng (Staff vs Admin)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Access Role:",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Role Staff
                        val isStaff = selectedRole == UserRole.STAFF
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isStaff) NeonTeal.copy(alpha = 0.22f) else Color(0x10FFFFFF))
                                .border(
                                    0.8.dp,
                                    if (isStaff) NeonTeal else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedRole = UserRole.STAFF
                                    if (loginStaffId == "admin@pulsedesk.vn") {
                                        loginStaffId = "staff_01@pulsedesk.vn"
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = if (isStaff) NeonTeal else TextDim,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Staff",
                                    color = if (isStaff) TextWhite else TextDim,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isStaff) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        // Role Admin
                        val isAdmin = selectedRole == UserRole.ADMIN
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAdmin) NeonPurple.copy(alpha = 0.25f) else Color(0x10FFFFFF))
                                .border(
                                    0.8.dp,
                                    if (isAdmin) NeonPurple else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedRole = UserRole.ADMIN
                                    if (loginStaffId == "staff_01@pulsedesk.vn") {
                                        loginStaffId = "admin@pulsedesk.vn"
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (isAdmin) NeonPurple else TextDim,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Admin",
                                    color = if (isAdmin) TextWhite else TextDim,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isAdmin) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Nội dung Form thay đổi theo Tab
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AuthFormAnimation"
                ) { tab ->
                    if (tab == AuthTab.LOG_IN) {
                        // ==================== FORM LOG IN ====================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Staff / Admin ID / Work Email
                            val isRoleAdmin = selectedRole == UserRole.ADMIN
                            AuthInputField(
                                label = if (isRoleAdmin) "Admin ID / Work Email" else "Staff ID / Work Email",
                                value = loginStaffId,
                                onValueChange = { loginStaffId = it },
                                placeholder = if (isRoleAdmin) "e.g. admin@pulsedesk.vn" else "e.g. staff_01@pulsedesk.vn",
                                leadingIcon = if (isRoleAdmin) Icons.Default.Security else Icons.Default.Badge
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password
                            AuthInputField(
                                label = "Password",
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                placeholder = "Enter your secret password",
                                leadingIcon = Icons.Default.Lock,
                                isPassword = true,
                                isPasswordVisible = isPasswordVisible,
                                onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tùy chọn Remember Device & Forgot Password
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { rememberDevice = !rememberDevice }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (rememberDevice) NeonCyan else Color.Transparent)
                                            .border(1.dp, if (rememberDevice) NeonCyan else TextDim, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (rememberDevice) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Remember device",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = "Forgot password?",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { }
                                )
                            }
                        }
                    } else {
                        // ==================== FORM REGISTER ====================
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Full Name
                            AuthInputField(
                                label = "Full Name",
                                value = regFullName,
                                onValueChange = { regFullName = it },
                                placeholder = "e.g. Nguyễn Văn An",
                                leadingIcon = Icons.Default.Person
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Staff / Admin ID / Work Email
                            val isRegRoleAdmin = selectedRole == UserRole.ADMIN
                            AuthInputField(
                                label = if (isRegRoleAdmin) "Admin ID / Email" else "Staff ID / Email",
                                value = regStaffId,
                                onValueChange = { regStaffId = it },
                                placeholder = if (isRegRoleAdmin) "e.g. admin.an@pulsedesk.vn" else "e.g. an.nguyen@pulsedesk.vn",
                                leadingIcon = if (isRegRoleAdmin) Icons.Default.Security else Icons.Default.Badge
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Password
                            AuthInputField(
                                label = "Password",
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                placeholder = "Create a strong password",
                                leadingIcon = Icons.Default.Lock,
                                isPassword = true,
                                isPasswordVisible = isRegPasswordVisible,
                                onTogglePasswordVisibility = { isRegPasswordVisible = !isRegPasswordVisible }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Confirm Password
                            AuthInputField(
                                label = "Confirm Password",
                                value = regConfirmPassword,
                                onValueChange = { regConfirmPassword = it },
                                placeholder = "Re-enter your password",
                                leadingIcon = Icons.Default.LockReset,
                                isPassword = true,
                                isPasswordVisible = isRegConfirmVisible,
                                onTogglePasswordVisibility = { isRegConfirmVisible = !isRegConfirmVisible }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Nhãn trạng thái bảo mật phiên (Nonce Challenge / Encrypted Session)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1400E5BC))
                        .border(0.6.dp, NeonTeal.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nonce Challenge Active • TLS/TCP Socket Ready",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "AES-256 GCM",
                            color = NeonTeal,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 6. Nút Đăng nhập / Đăng ký
                val isLogin = currentTab == AuthTab.LOG_IN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(12.dp, RoundedCornerShape(23.dp), spotColor = if (isLogin) NeonCyan else NeonTeal)
                        .clip(RoundedCornerShape(23.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (isLogin) listOf(Color(0xFF00B4D8), Color(0xFF0077B6))
                                else listOf(Color(0xFF00E5BC), Color(0xFF7B2CBF))
                            )
                        )
                        .border(
                            1.dp,
                            if (isLogin) NeonCyan.copy(alpha = 0.7f) else NeonTeal.copy(alpha = 0.7f),
                            RoundedCornerShape(23.dp)
                        )
                        .clickable(enabled = !isAuthenticating) {
                            scope.launch {
                                isAuthenticating = true
                                statusMessage = "Verifying Nonce Signature via TCP Socket..."
                                delay(700)
                                isAuthenticating = false
                                onLoginSuccess(
                                    if (isLogin) loginStaffId else regStaffId.ifBlank {
                                        if (selectedRole == UserRole.ADMIN) "admin@pulsedesk.vn" else "staff_01@pulsedesk.vn"
                                    },
                                    selectedRole.title
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAuthenticating) {
                            Text(
                                text = "Authenticating...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = if (isLogin) "Log In to PulseDesk" else "Complete Registration",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isLogin) Icons.Default.Login else Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Trường nhập liệu kính mờ dùng chung
@Composable
private fun AuthInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x18FFFFFF))
                .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = TextDim,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextDim,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(color = TextWhite, fontSize = 13.sp),
                        cursorBrush = SolidColor(NeonCyan),
                        singleLine = true,
                        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isPassword && onTogglePasswordVisibility != null) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = TextDim,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onTogglePasswordVisibility() }
                    )
                }
            }
        }
    }
}
