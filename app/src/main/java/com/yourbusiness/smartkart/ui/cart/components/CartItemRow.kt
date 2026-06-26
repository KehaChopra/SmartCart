package com.yourbusiness.smartkart.ui.cart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourbusiness.smartkart.data.model.SessionItem
import com.yourbusiness.smartkart.ui.cart.formatRupee
import com.yourbusiness.smartkart.ui.theme.SmartKartDarkGreenLight
import com.yourbusiness.smartkart.ui.theme.SmartKartGreen
import com.yourbusiness.smartkart.ui.theme.SmartKartGreenLight

@Composable
fun CartItemRow(
    item: SessionItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            ProductThumbnail(
                name = item.name,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${formatRupee(item.price)} / unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartKartGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Qty: ${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatRupee(item.lineTotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Top)
            )
        }
    }
}

@Composable
private fun ProductThumbnail(
    name: String,
    modifier: Modifier = Modifier
) {
    val icon = productIconForName(name)

    val thumbnailBackground = if (isSystemInDarkTheme()) SmartKartDarkGreenLight else SmartKartGreenLight

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(thumbnailBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SmartKartGreen,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun productIconForName(name: String): ImageVector {
    val normalized = name.lowercase()
    return when {
        "milk" in normalized || "drink" in normalized || "juice" in normalized -> {
            Icons.Outlined.LocalDrink
        }
        "bread" in normalized || "bake" in normalized || "cake" in normalized -> {
            Icons.Outlined.BakeryDining
        }
        else -> Icons.Outlined.ShoppingBag
    }
}
