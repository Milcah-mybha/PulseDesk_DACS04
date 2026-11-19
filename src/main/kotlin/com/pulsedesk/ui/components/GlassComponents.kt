package com.pulsedesk.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsedesk.ui.theme.*

// Modifier kính mờ (Liquid Frosted Glass) cho Compose Desktop
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassSurface,
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 10.dp,
    shadowColor: Color = GlassShadow
): Modifier = this
    .shadow(
        elevation = shadowElevation,
        shape = shape,
        clip = false,
        ambientColor = Color(0x30000000),
        spotColor = shadowColor
    )
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                backgroundColor.copy(alpha = 0.28f),
                backgroundColor.copy(alpha = 0.08f)
            )
        )
    )
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                GlassHighlight,
                Color(0x18FFFFFF),
                NeonCyan.copy(alpha = 0.35f)
            )
        ),
        shape = shape
    )

// Nền tổng thể chuyển sắc gradient (xanh dương đậm, tím, mòng két) với các mảng sáng phát xạ
@Composable
fun PulseDeskRootBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(BgNavy, BgDeepBlue, BgPurple, BgTeal)
                )
            )
    ) {
        // Vùng sáng tím phát xạ góc trên phải
        Canvas(
            modifier = Modifier
                .size(600.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-100).dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.30f), Color(0x107209B7), Color.Transparent),
                    radius = size.width / 2
                )
            )
        }

        // Vùng sáng mòng két/Cyan phát xạ giữa màn hình
        Canvas(
            modifier = Modifier
                .size(650.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-180).dp, y = 50.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonTeal.copy(alpha = 0.28f), Color(0x0800B4D8), Color.Transparent),
                    radius = size.width / 2
                )
            )
        }

        // Vùng sáng hồng phát xạ góc dưới phải
        Canvas(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 140.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPink.copy(alpha = 0.22f), Color(0x08FF007F), Color.Transparent),
                    radius = size.width / 2
                )
            )
        }

        content()
    }
}

// Chấm trạng thái Online nhấp nháy phát sáng
@Composable
fun GlowingOnlineDot(size: Dp = 10.dp) {
    val transition = rememberInfiniteTransition(label = "onlinePulse")
    val haloScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "scale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier.size(size * 1.8f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 1.5f)
                .graphicsLayer {
                    scaleX = haloScale
                    scaleY = haloScale
                    alpha = haloAlpha
                }
                .clip(CircleShape)
                .background(NeonGreen)
        )
        Box(
            modifier = Modifier
                .size(size)
                .shadow(6.dp, CircleShape, spotColor = NeonGreen)
                .clip(CircleShape)
                .background(NeonGreen)
                .border(1.5.dp, Color(0xFF04060E), CircleShape)
        )
    }
}

// Huy hiệu tin chưa đọc phát sáng
@Composable
fun GlowingUnreadBadge(count: Int) {
    if (count <= 0) return
    Box(
        modifier = Modifier
            .shadow(10.dp, CircleShape, spotColor = NeonCyan)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(NeonCyan, Color(0xFF0055FF))))
            .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
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
