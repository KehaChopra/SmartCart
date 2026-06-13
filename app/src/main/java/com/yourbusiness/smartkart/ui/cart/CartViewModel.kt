package com.yourbusiness.smartkart.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourbusiness.smartkart.data.model.ShoppingSession
import com.yourbusiness.smartkart.data.model.calculateTotalAmount
import com.yourbusiness.smartkart.data.repository.CartRepository
import com.yourbusiness.smartkart.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CartViewModel(
    private val cartId: String,
    private val sessionRepository: SessionRepository = SessionRepository(),
    private val cartRepository: CartRepository = CartRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _removingBarcodes = MutableStateFlow<Set<String>>(emptySet())
    val removingBarcodes: StateFlow<Set<String>> = _removingBarcodes.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        startSessionListener()
    }

    private fun startSessionListener() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading

            sessionRepository.getSessionIdForCart(cartId)
                .onSuccess { sessionId ->
                    sessionRepository.observeSession(sessionId) { result ->
                        result.fold(
                            onSuccess = { session -> updateUiFromSession(session) },
                            onFailure = { exception ->
                                _uiState.value = CartUiState.Error(
                                    sessionRepository.mapExceptionToMessage(exception)
                                )
                            }
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.value = CartUiState.Error(
                        sessionRepository.mapExceptionToMessage(exception)
                    )
                }
        }
    }

    fun removeItem(barcode: String) {
        if (_removingBarcodes.value.contains(barcode)) return

        viewModelScope.launch {
            _removingBarcodes.update { it + barcode }

            cartRepository.removeItemFromCart(cartId, barcode)
                .onSuccess {
                    _removingBarcodes.update { it - barcode }
                }
                .onFailure { exception ->
                    _removingBarcodes.update { it - barcode }
                    _snackbarMessage.emit(
                        cartRepository.mapExceptionToMessage(exception)
                    )
                }
        }
    }

    fun retry() {
        sessionRepository.removeListener()
        startSessionListener()
    }

    override fun onCleared() {
        sessionRepository.removeListener()
        super.onCleared()
    }

    private fun updateUiFromSession(session: ShoppingSession) {
        _removingBarcodes.update { current ->
            current.filter { barcode ->
                session.items.any { it.barcode == barcode }
            }.toSet()
        }

        _uiState.value = if (session.items.isEmpty()) {
            CartUiState.Empty(
                cartId = session.cartId,
                sessionId = session.sessionId
            )
        } else {
            CartUiState.Success(
                cartId = session.cartId,
                sessionId = session.sessionId,
                items = session.items,
                totalAmount = session.items.calculateTotalAmount()
            )
        }
    }
}

class CartViewModelFactory(
    private val cartId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            return CartViewModel(cartId = cartId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
