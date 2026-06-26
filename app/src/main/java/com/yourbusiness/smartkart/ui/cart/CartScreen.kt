package com.yourbusiness.smartkart.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourbusiness.smartkart.data.model.SessionItem
import com.yourbusiness.smartkart.ui.cart.components.CartItemRow
import com.yourbusiness.smartkart.ui.theme.SmartKartGreen
import com.yourbusiness.smartkart.ui.theme.SmartKartTheme

@Composable
fun CartScreen(
    cartId: String,
    sessionId: String? = null,
    onCheckout: (totalAmount: Double, items: List<SessionItem>, sessionId: String) -> Unit,
    onNavigateToScanner: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = viewModel(
        key = "cart_${cartId}_${sessionId.orEmpty()}",
        factory = CartViewModelFactory(cartId, sessionId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigationState by viewModel.navigationState.collectAsStateWithLifecycle()
    val isAbandoning by viewModel.isAbandoning.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLeaveCartDialog by remember { mutableStateOf(false) }
    val hasActiveSession = uiState is CartUiState.Empty || uiState is CartUiState.Success

    LaunchedEffect(cartId, sessionId) {
        viewModel.reloadSession()
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(navigationState) {
        when (val state = navigationState) {
            is CartNavigationState.NavigateToScanner -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.onNavigationHandled()
                onNavigateToScanner()
            }

            CartNavigationState.Idle -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CartBottomBar(
                uiState = uiState,
                onCheckout = {
                    val successState = uiState as? CartUiState.Success ?: return@CartBottomBar
                    onCheckout(successState.totalAmount, successState.items, successState.sessionId)
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CartHeader(
                cartId = cartId,
                showExitButton = hasActiveSession && !isAbandoning,
                onExitClick = { showLeaveCartDialog = true }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    CartUiState.Loading -> CartLoadingContent()

                    is CartUiState.Empty -> CartEmptyContent(
                        cartId = state.cartId,
                        modifier = Modifier.fillMaxSize()
                    )

                    is CartUiState.Success -> CartSuccessContent(
                        items = state.items,
                        modifier = Modifier.fillMaxSize()
                    )

                    is CartUiState.Error -> CartErrorContent(
                        message = state.message,
                        onRetry = viewModel::retry,
                        onSignOut = onSignOut,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isAbandoning) {
                    CartAbandoningOverlay()
                }
            }
        }
    }

    if (showLeaveCartDialog) {
        LeaveCartDialog(
            onDismiss = { showLeaveCartDialog = false },
            onConfirm = {
                showLeaveCartDialog = false
                viewModel.abandonCart()
            }
        )
    }
}

@Composable
private fun CartHeader(
    cartId: String,
    showExitButton: Boolean,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Cart",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SmartKartGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cartId,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartKartGreen
                        )
                    }
                }

                if (showExitButton) {
                    IconButton(onClick = onExitClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                            contentDescription = "Leave cart",
                            tint = SmartKartGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SmartKartGreen)
        )
    }
}

@Composable
private fun LeaveCartDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Leave Cart?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to exit? " +
                    "Your cart will be cleared and unlinked from your account."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Leave Cart",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Stay",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun CartAbandoningOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SmartKartGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Leaving cart…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        CircularProgressIndicator(color = SmartKartGreen)
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
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🛒",
            style = MaterialTheme.typography.displaySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scan items using the cart scanner to start shopping",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your cart $cartId is ready. Items will appear here as you scan them.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CartSuccessContent(
    items: List<SessionItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = items,
            key = { item -> item.barcode }
        ) { item ->
            CartItemRow(item = item)
        }

        item {
            Spacer(modifier = Modifier.height(96.dp))
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
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Could not load cart",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SmartKartGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Try again", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text("Sign out", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CartBottomBar(
    uiState: CartUiState,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val successState = uiState as? CartUiState.Success
    val totalAmount = successState?.totalAmount ?: 0.0
    val itemCount = successState?.items?.sumOf { it.quantity } ?: 0
    val canCheckout = successState != null && totalAmount > 0.0
    val showBar = uiState is CartUiState.Success || uiState is CartUiState.Empty

    if (!showBar) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (itemCount == 1) "1 item" else "$itemCount items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = formatRupee(totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmartKartGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canCheckout,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartKartGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (canCheckout) "Proceed to Checkout" else "Add items to checkout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
                    SessionItem("111", "Organic Whole Milk", 68.0, 2),
                    SessionItem("222", "Bread", 40.0, 1)
                ),
                totalAmount = 176.0
            ),
            onCheckout = {}
        )
    }
}
