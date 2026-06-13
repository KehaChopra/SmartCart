package com.yourbusiness.smartkart.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourbusiness.smartkart.data.CartIdParser
import com.yourbusiness.smartkart.data.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QrScannerViewModel(
    private val cartRepository: CartRepository = CartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<QrScannerUiState>(QrScannerUiState.Scanning)
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    private var isProcessingScan = false

    val isCameraScanningEnabled: Boolean
        get() = _uiState.value is QrScannerUiState.Scanning && !isProcessingScan

    fun onBarcodeDetected(rawValue: String) {
        if (!isCameraScanningEnabled) return

        val cartId = CartIdParser.parse(rawValue)
        if (cartId.isNullOrBlank()) return

        isProcessingScan = true
        viewModelScope.launch {
            _uiState.value = QrScannerUiState.Loading

            cartRepository.bindCartToUser(cartId)
                .onSuccess { boundCartId ->
                    _uiState.value = QrScannerUiState.Success(boundCartId)
                }
                .onFailure { exception ->
                    isProcessingScan = false
                    val message = cartRepository.mapExceptionToMessage(exception)
                    _uiState.value = QrScannerUiState.Error(
                        cartRepository.formatBindError(message, cartId)
                    )
                }
        }
    }

    fun retryScanning() {
        isProcessingScan = false
        _uiState.value = QrScannerUiState.Scanning
    }
}
