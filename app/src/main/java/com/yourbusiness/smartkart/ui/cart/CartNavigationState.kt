package com.yourbusiness.smartkart.ui.cart

sealed class CartNavigationState {
    data object Idle : CartNavigationState()

    data class NavigateToScanner(
        val message: String = "Cart session ended. Please scan a cart to continue."
    ) : CartNavigationState()
}
