package com.yourbusiness.smartkart.ui.cart

sealed class CartCheckUiState {
    data object Idle : CartCheckUiState()
    data object Loading : CartCheckUiState()
    data object NavigateToScanner : CartCheckUiState()
    data class NavigateToCart(val cartId: String) : CartCheckUiState()
    data class Error(val message: String) : CartCheckUiState()
}
