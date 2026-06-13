package com.yourbusiness.smartkart.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourbusiness.smartkart.data.model.SessionItem
import com.yourbusiness.smartkart.ui.cart.components.CartItemRow
import com.yourbusiness.smartkart.ui.theme.SmartKartTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartId: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = viewModel(factory = CartViewModelFactory(cartId))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val removingBarcodes by viewModel.removingBarcodes.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Your Cart")
                        Text(
                            text = cartId,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CartBottomBar(
                uiState = uiState,
                onCheckout = { /* Checkout will be added in a future step */ }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                CartUiState.Loading -> CartLoadingContent()

                is CartUiState.Empty -> CartEmptyContent(
                    cartId = state.cartId,
                    modifier = Modifier.fillMaxSize()
                )

                is CartUiState.Success -> CartSuccessContent(
                    items = state.items,
                    removingBarcodes = removingBarcodes,
                    onRemoveItem = viewModel::removeItem,
                    modifier = Modifier.fillMaxSize()
                )

                is CartUiState.Error -> CartErrorContent(
                    message = state.message,
                    onRetry = viewModel::retry,
                    onSignOut = onSignOut,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CartLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading your cart…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CartEmptyContent(
    cartId: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🛒",
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scan items to start shopping",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your cart $cartId is ready. Add products by scanning their barcodes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CartSuccessContent(
    items: List<SessionItem>,
    removingBarcodes: Set<String>,
    onRemoveItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = items,
            key = { item -> item.barcode }
        ) { item ->
            CartItemRow(
                item = item,
                isRemoving = removingBarcodes.contains(item.barcode),
                onRemove = { onRemoveItem(item.barcode) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CartErrorContent(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Could not load cart",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try again")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onSignOut) {
            Text("Sign out")
        }
    }
}

@Composable
private fun CartBottomBar(
    uiState: CartUiState,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount = when (uiState) {
        is CartUiState.Success -> uiState.totalAmount
        else -> 0.0
    }

    val canCheckout = uiState is CartUiState.Success && totalAmount > 0.0
    val showBar = uiState is CartUiState.Success || uiState is CartUiState.Empty

    if (!showBar) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            RowTotal(totalAmount = totalAmount)

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canCheckout
            ) {
                Text(
                    text = if (canCheckout) "Checkout" else "Add items to checkout",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun RowTotal(
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        RowAmountLine(label = "Item total", amount = totalAmount)
        Spacer(modifier = Modifier.height(8.dp))
        RowAmountLine(
            label = "To pay",
            amount = totalAmount,
            emphasized = true
        )
    }
}

@Composable
private fun RowAmountLine(
    label: String,
    amount: Double,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasized) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            text = formatRupee(amount),
            style = if (emphasized) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.Bold,
            color = if (emphasized) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CartScreenSuccessPreview() {
    SmartKartTheme {
        CartBottomBar(
            uiState = CartUiState.Success(
                cartId = "CART_001",
                sessionId = "session-1",
                items = listOf(
                    SessionItem("111", "Milk", 55.0, 2),
                    SessionItem("222", "Bread", 40.0, 1)
                ),
                totalAmount = 150.0
            ),
            onCheckout = {}
        )
    }
}
