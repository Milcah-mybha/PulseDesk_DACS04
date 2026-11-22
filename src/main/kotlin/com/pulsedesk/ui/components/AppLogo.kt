package com.pulsedesk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import java.io.InputStream

object LogoLoader {
    private var cachedBitmap: ImageBitmap? = null

    fun getLogoBitmap(): ImageBitmap? {
        if (cachedBitmap != null) return cachedBitmap
        return try {
            val stream: InputStream? = LogoLoader::class.java.classLoader.getResourceAsStream("assets/logo.png")
            if (stream != null) {
                val bytes = stream.readBytes()
                val skiaImage = Image.makeFromEncoded(bytes)
                cachedBitmap = skiaImage.toComposeImageBitmap()
                cachedBitmap
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val bitmap = remember { LogoLoader.getLogoBitmap() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "PulseDesk Logo",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f))
        )
    }
}
