package com.yourbusiness.smartkart.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CartGateScreen(
    viewModel: CartCheckViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToCart: (cartId: String) -> Unit
) {
    val navigationState by viewModel.navigationState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (navigationState is CartCheckUiState.Idle) {
            viewModel.checkActiveCart()
        }
    }

    LaunchedEffect(navigationState) {
        when (val state = navigationState) {
            CartCheckUiState.NavigateToScanner -> {
                viewModel.onNavigationHandled()
                onNavigateToScanner()
            }

            is CartCheckUiState.NavigateToCart -> {
                viewModel.onNavigationHandled()
                onNavigateToCart(state.cartId)
            }

            else -> Unit
        }
    }

    when (val state = navigationState) {
        CartCheckUiState.Idle,
        CartCheckUiState.Loading,
        CartCheckUiState.NavigateToScanner,
        is CartCheckUiState.NavigateToCart -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Checking your cart…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is CartCheckUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Could not check your cart",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = viewModel::checkActiveCart) {
                    Text("Try again")
                }
            }
        }
    }
}
