package com.yourbusiness.smartkart.ui.cart

sealed class QrScannerUiState {
    data object Scanning : QrScannerUiState()
    data object Loading : QrScannerUiState()
    data class Success(val cartId: String) : QrScannerUiState()
    data class Error(val message: String) : QrScannerUiState()
}
