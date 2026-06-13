package com.yourbusiness.smartkart.ui.cart

import com.yourbusiness.smartkart.data.model.SessionItem

sealed class CartUiState {
    data object Loading : CartUiState()

    data class Success(
        val cartId: String,
        val sessionId: String,
        val items: List<SessionItem>,
        val totalAmount: Double
    ) : CartUiState()

    data class Empty(
        val cartId: String,
        val sessionId: String
    ) : CartUiState()

    data class Error(val message: String) : CartUiState()
}
