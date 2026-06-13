package com.yourbusiness.smartkart.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.yourbusiness.smartkart.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CartCheckViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _navigationState = MutableStateFlow<CartCheckUiState>(CartCheckUiState.Idle)
    val navigationState: StateFlow<CartCheckUiState> = _navigationState.asStateFlow()

    fun checkActiveCart() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _navigationState.value = CartCheckUiState.Error(
                "You are not signed in. Please log in again."
            )
            return
        }

        if (_navigationState.value is CartCheckUiState.Loading) return

        viewModelScope.launch {
            _navigationState.value = CartCheckUiState.Loading

            userRepository.getActiveCart(uid)
                .onSuccess { activeCart ->
                    _navigationState.value = if (activeCart.isNullOrBlank()) {
                        CartCheckUiState.NavigateToScanner
                    } else {
                        CartCheckUiState.NavigateToCart(activeCart)
                    }
                }
                .onFailure { exception ->
                    _navigationState.value = CartCheckUiState.Error(
                        userRepository.mapExceptionToMessage(exception)
                    )
                }
        }
    }

    fun prepareForCartCheck() {
        _navigationState.value = CartCheckUiState.Idle
    }

    fun resetForSignOut() {
        _navigationState.value = CartCheckUiState.Idle
    }

    fun onNavigationHandled() {
        _navigationState.value = CartCheckUiState.Idle
    }
}
