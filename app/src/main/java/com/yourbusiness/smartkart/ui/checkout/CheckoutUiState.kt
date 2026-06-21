package com.yourbusiness.smartkart.ui.checkout

sealed class CheckoutUiState {
    data class Ready(val totalAmount: Double) : CheckoutUiState()
    data object Processing : CheckoutUiState()
    data class Success(val totalAmount: Double) : CheckoutUiState()
    data class Error(val message: String) : CheckoutUiState()
}
