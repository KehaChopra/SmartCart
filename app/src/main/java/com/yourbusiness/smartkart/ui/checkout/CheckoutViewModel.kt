package com.yourbusiness.smartkart.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.yourbusiness.smartkart.data.repository.PaymentRepository
import com.yourbusiness.smartkart.payment.RazorpayPaymentBridge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CheckoutViewModel(
    private val cartId: String,
    private val sessionId: String,
    private val totalAmount: Double,
    private val paymentRepository: PaymentRepository = PaymentRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Loading)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _paymentSdkConfig = MutableSharedFlow<PaymentSdkConfig>(extraBufferCapacity = 1)
    val paymentSdkConfig: SharedFlow<PaymentSdkConfig> = _paymentSdkConfig.asSharedFlow()

    init {
        _uiState.value = CheckoutUiState.Loading

        viewModelScope.launch {
            RazorpayPaymentBridge.events.collect { event ->
                when (event) {
                    is RazorpayPaymentBridge.SdkEvent.Success -> {
                        onPaymentSuccess(
                            orderId = event.orderId,
                            paymentId = event.paymentId,
                            signature = event.signature
                        )
                    }

                    is RazorpayPaymentBridge.SdkEvent.Failure -> {
                        onPaymentError(event.code, event.description)
                    }
                }
            }
        }
    }

    fun initiatePayment() {
        val currentState = _uiState.value
        if (
            currentState is CheckoutUiState.CreatingOrder ||
            currentState is CheckoutUiState.AwaitingPayment ||
            currentState is CheckoutUiState.VerifyingPayment
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.value = CheckoutUiState.CreatingOrder

            val userPhone = auth.currentUser?.phoneNumber.orEmpty()
            paymentRepository.createOrder(
                cartId = cartId,
                sessionId = sessionId,
                userPhone = userPhone
            ).onSuccess { config ->
                RazorpayPaymentBridge.inFlightOrderId = config.orderId
                _uiState.value = CheckoutUiState.AwaitingPayment
                _paymentSdkConfig.emit(config)
            }.onFailure { exception ->
                _uiState.value = CheckoutUiState.Error(
                    paymentRepository.mapExceptionToMessage(exception)
                )
            }
        }
    }

    fun onPaymentSuccess(orderId: String, paymentId: String, signature: String) {
        if (_uiState.value is CheckoutUiState.VerifyingPayment) return

        viewModelScope.launch {
            _uiState.value = CheckoutUiState.VerifyingPayment

            paymentRepository.verifyPayment(
                orderId = orderId,
                paymentId = paymentId,
                signature = signature
            ).onSuccess {
                _uiState.value = CheckoutUiState.PaymentSuccess(cartId)
            }.onFailure { exception ->
                _uiState.value = CheckoutUiState.PaymentFailed(
                    paymentRepository.mapExceptionToMessage(exception)
                )
            }
        }
    }

    fun onPaymentError(code: Int, description: String) {
        if (_uiState.value is CheckoutUiState.VerifyingPayment) return

        val reason = description.takeIf { it.isNotBlank() }
            ?: "Payment was cancelled or failed (code $code)."
        _uiState.value = CheckoutUiState.PaymentFailed(reason)
    }

    fun retry() {
        _uiState.value = CheckoutUiState.Loading
    }

    fun totalAmount(): Double = totalAmount
}

class CheckoutViewModelFactory(
    private val cartId: String,
    private val sessionId: String,
    private val totalAmount: Double
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckoutViewModel::class.java)) {
            return CheckoutViewModel(
                cartId = cartId,
                sessionId = sessionId,
                totalAmount = totalAmount
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
