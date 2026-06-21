package com.yourbusiness.smartkart.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourbusiness.smartkart.data.repository.CheckoutRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val cartId: String,
    private val totalAmount: Double,
    private val checkoutRepository: CheckoutRepository = CheckoutRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Ready(totalAmount))
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun payNow() {
        if (_uiState.value is CheckoutUiState.Processing) return

        viewModelScope.launch {
            _uiState.value = CheckoutUiState.Processing

            // Placeholder for Razorpay — simulate brief payment delay
            delay(SIMULATED_PAYMENT_DELAY_MS)

            checkoutRepository.completeCheckout(cartId)
                .onSuccess {
                    _uiState.value = CheckoutUiState.Success(totalAmount)
                }
                .onFailure { exception ->
                    _uiState.value = CheckoutUiState.Error(
                        checkoutRepository.mapExceptionToMessage(exception)
                    )
                }
        }
    }

    fun retry() {
        _uiState.value = CheckoutUiState.Ready(totalAmount)
    }

    companion object {
        private const val SIMULATED_PAYMENT_DELAY_MS = 1_500L
    }
}

class CheckoutViewModelFactory(
    private val cartId: String,
    private val totalAmount: Double
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckoutViewModel::class.java)) {
            return CheckoutViewModel(cartId = cartId, totalAmount = totalAmount) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
