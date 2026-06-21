package com.yourbusiness.smartkart.ui.cart.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourbusiness.smartkart.ui.theme.SmartKartBackground
import com.yourbusiness.smartkart.ui.theme.SmartKartGreen

@Composable
fun SmartKartScannerBrandHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SmartKartGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = null,
                tint = SmartKartBackground,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "SmartKart",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SmartKartBackground,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun QrViewfinderOverlay(
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cornerLength = 36.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val cornerColor = SmartKartGreen

        fun drawCorner(
            startX: Float,
            startY: Float,
            horizontalDirection: Float,
            verticalDirection: Float
        ) {
            drawLine(
                color = cornerColor,
                start = Offset(startX, startY),
                end = Offset(startX + cornerLength * horizontalDirection, startY),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = Offset(startX, startY),
                end = Offset(startX, startY + cornerLength * verticalDirection),
                strokeWidth = strokeWidth
            )
        }

        drawCorner(0f, 0f, 1f, 1f)
        drawCorner(size.width, 0f, -1f, 1f)
        drawCorner(0f, size.height, 1f, -1f)
        drawCorner(size.width, size.height, -1f, -1f)

        if (isAnimating) {
            val lineY = size.height * scanLineProgress
            drawLine(
                color = SmartKartGreen.copy(alpha = 0.85f),
                start = Offset(size.width * 0.08f, lineY),
                end = Offset(size.width * 0.92f, lineY),
                strokeWidth = 2.5.dp.toPx()
            )
            drawLine(
                color = SmartKartGreen.copy(alpha = 0.35f),
                start = Offset(size.width * 0.08f, lineY),
                end = Offset(size.width * 0.92f, lineY),
                strokeWidth = 8.dp.toPx()
            )
        }
    }
}
