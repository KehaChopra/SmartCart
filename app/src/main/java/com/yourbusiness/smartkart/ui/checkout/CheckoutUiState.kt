package com.yourbusiness.smartkart.ui.checkout

sealed class CheckoutUiState {
    data object Loading : CheckoutUiState()
    data object CreatingOrder : CheckoutUiState()
    data object AwaitingPayment : CheckoutUiState()
    data object VerifyingPayment : CheckoutUiState()
    data class PaymentSuccess(val cartId: String) : CheckoutUiState()
    data class PaymentFailed(val reason: String) : CheckoutUiState()
    data class Error(val message: String) : CheckoutUiState()
}
