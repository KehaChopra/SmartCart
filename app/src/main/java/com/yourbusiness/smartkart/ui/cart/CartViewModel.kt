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
    private val knownSessionId: String? = null,
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

    private val _isAbandoning = MutableStateFlow(false)
    val isAbandoning: StateFlow<Boolean> = _isAbandoning.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var hasHandledSessionEnded = false

    fun reloadSession() {
        hasHandledSessionEnded = false
        _isAbandoning.value = false
        sessionRepository.removeListener()
        startSessionListener()
    }

    private fun startSessionListener() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading

            val bootstrapResult = if (!knownSessionId.isNullOrBlank()) {
                sessionRepository.loadSessionOnce(knownSessionId)
            } else {
                sessionRepository.loadCartSessionOnce(cartId)
            }

            bootstrapResult.fold(
                onSuccess = { session -> updateUiFromSession(session) },
                onFailure = { exception ->
                    if (sessionRepository.isSessionEndedError(exception)) {
                        handleSessionEnded()
                        return@launch
                    }
                    _uiState.value = CartUiState.Error(
                        sessionRepository.mapExceptionToMessage(exception)
                    )
                }
            )

            val onSessionUpdate: (Result<ShoppingSession>) -> Unit = { result ->
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

            if (!knownSessionId.isNullOrBlank()) {
                sessionRepository.observeSession(knownSessionId, onSessionUpdate)
            } else {
                sessionRepository.observeCartSession(cartId, onSessionUpdate)
            }
        }
    }

    fun abandonCart() {
        if (_isAbandoning.value) return

        val sessionId = when (val state = _uiState.value) {
            is CartUiState.Empty -> state.sessionId
            is CartUiState.Success -> state.sessionId
            else -> return
        }

        viewModelScope.launch {
            _isAbandoning.value = true

            cartRepository.abandonCart(cartId, sessionId)
                .onSuccess {
                    sessionRepository.removeListener()
                    val uid = auth.currentUser?.uid
                    if (!uid.isNullOrBlank()) {
                        userRepository.clearActiveCart(uid)
                    }
                    _navigationState.value = CartNavigationState.NavigateToScanner(
                        message = "Cart session ended successfully."
                    )
                }
                .onFailure { exception ->
                    _snackbarMessage.emit(
                        cartRepository.mapExceptionToMessage(exception)
                    )
                }

            _isAbandoning.value = false
        }
    }

    fun retry() {
        reloadSession()
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
    private val cartId: String,
    private val sessionId: String? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            return CartViewModel(
                cartId = cartId,
                knownSessionId = sessionId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
