package com.yourbusiness.smartkart.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.yourbusiness.smartkart.data.model.ShoppingSession
import com.yourbusiness.smartkart.data.model.calculateTotalAmount
import com.yourbusiness.smartkart.data.repository.CartRepository
import com.yourbusiness.smartkart.data.repository.SessionRepository
import com.yourbusiness.smartkart.data.repository.UserRepository
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
    private val cartRepository: CartRepository = CartRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _navigationState = MutableStateFlow<CartNavigationState>(CartNavigationState.Idle)
    val navigationState: StateFlow<CartNavigationState> = _navigationState.asStateFlow()

    private val _removingBarcodes = MutableStateFlow<Set<String>>(emptySet())
    val removingBarcodes: StateFlow<Set<String>> = _removingBarcodes.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var hasHandledSessionEnded = false

    init {
        startSessionListener()
    }

    private fun startSessionListener() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading

            sessionRepository.observeCartSession(cartId) { result ->
                result.fold(
                    onSuccess = { session -> updateUiFromSession(session) },
                    onFailure = { exception ->
                        if (sessionRepository.isSessionEndedError(exception)) {
                            handleSessionEnded()
                        } else {
                            _uiState.value = CartUiState.Error(
                                sessionRepository.mapExceptionToMessage(exception)
                            )
                        }
                    }
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
        hasHandledSessionEnded = false
        sessionRepository.removeListener()
        startSessionListener()
    }

    fun onNavigationHandled() {
        _navigationState.value = CartNavigationState.Idle
    }

    override fun onCleared() {
        sessionRepository.removeListener()
        super.onCleared()
    }

    private fun handleSessionEnded() {
        if (hasHandledSessionEnded) return
        hasHandledSessionEnded = true

        sessionRepository.removeListener()
        _uiState.value = CartUiState.Loading

        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                userRepository.clearActiveCart(uid)
            }

            _navigationState.value = CartNavigationState.NavigateToScanner()
        }
    }

    private fun updateUiFromSession(session: ShoppingSession) {
        if (sessionRepository.isSessionInactive(session)) {
            handleSessionEnded()
            return
        }

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
