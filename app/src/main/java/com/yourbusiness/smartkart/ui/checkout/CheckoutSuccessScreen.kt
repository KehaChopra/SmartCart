package com.yourbusiness.smartkart.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourbusiness.smartkart.ui.cart.formatRupee
import com.yourbusiness.smartkart.ui.theme.SmartKartGreen
import com.yourbusiness.smartkart.ui.theme.SmartKartGreenPill
import com.yourbusiness.smartkart.ui.theme.SmartKartTheme
import kotlin.math.abs

@Composable
fun CheckoutSuccessScreen(
    cartId: String,
    totalAmount: Double,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactionId = remember(totalAmount, cartId) {
        generateTransactionId(cartId, totalAmount)
    }
    val displayTotal = remember(totalAmount) {
        val subtotal = totalAmount
        val gst = subtotal * GST_RATE
        subtotal + gst
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SuccessIcon()

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Payment Successful!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your cart is now unlocked.\nCollect your items.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = SmartKartGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$cartId · Unlocked",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartKartGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TransactionDetailsCard(
                    amountPaid = displayTotal,
                    transactionId = transactionId
                )
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartKartGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SuccessIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SmartKartGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun TransactionDetailsCard(
    amountPaid: Double,
    transactionId: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TransactionRow(
                label = "Amount paid",
                value = formatRupee(amountPaid),
                valueColor = SmartKartGreen,
                emphasized = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            TransactionRow(
                label = "Transaction ID",
                value = transactionId,
                valueColor = MaterialTheme.colorScheme.onSurface,
                emphasized = true
            )
        }
    }
}

@Composable
private fun TransactionRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    emphasized: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

private fun generateTransactionId(cartId: String, totalAmount: Double): String {
    val seed = abs((cartId + totalAmount.toString()).hashCode())
    return "TXN${seed.toString().takeLast(7).padStart(7, '0')}"
}

private const val GST_RATE = 0.05

@Preview(showBackground = true)
@Composable
private fun CheckoutSuccessScreenPreview() {
    SmartKartTheme {
        CheckoutSuccessScreen(
            cartId = "CART_001",
            totalAmount = 365.0,
            onDone = {}
        )
    }
}
