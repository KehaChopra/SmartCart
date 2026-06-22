package com.yourbusiness.smartkart.ui.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yourbusiness.smartkart.R
import com.yourbusiness.smartkart.ui.theme.SmartKartGreen
import com.yourbusiness.smartkart.ui.theme.SmartKartGreenLight

@Composable
fun SmartKartBrandHeader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SmartKartGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_smartkart_logo),
                    contentDescription = "SmartKart logo",
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Smart")
                    }
                    withStyle(SpanStyle(color = SmartKartGreen, fontWeight = FontWeight.Bold)) {
                        append("Kart")
                    }
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Scan. Shop. Go.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SmartKartCartIllustration(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier.size(188.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer diffuse ring
        Box(
            modifier = Modifier
                .size(188.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x1A106326) else Color(0x0A106326))
        )
        // Middle ring
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x33106326) else Color(0x1A106326))
        )
        // Inner filled circle with logo
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x4D106326) else Color(0x33106326)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_smartkart_logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}
