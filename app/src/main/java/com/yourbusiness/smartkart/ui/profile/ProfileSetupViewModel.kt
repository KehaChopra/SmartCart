package com.yourbusiness.smartkart.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.yourbusiness.smartkart.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileSetupViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _gateState = MutableStateFlow<ProfileGateUiState>(ProfileGateUiState.Idle)
    val gateState: StateFlow<ProfileGateUiState> = _gateState.asStateFlow()

    private val _setupState = MutableStateFlow<ProfileSetupUiState>(ProfileSetupUiState.Idle)
    val setupState: StateFlow<ProfileSetupUiState> = _setupState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    fun checkUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _gateState.value = ProfileGateUiState.Error("You are not signed in. Please log in again.")
            return
        }

        if (_gateState.value is ProfileGateUiState.Loading) return

        viewModelScope.launch {
            _gateState.value = ProfileGateUiState.Loading

            userRepository.userProfileExists(uid)
                .onSuccess { exists ->
                    _gateState.value = if (exists) {
                        ProfileGateUiState.ProfileExists
                    } else {
                        ProfileGateUiState.NeedsProfileSetup
                    }
                }
                .onFailure { exception ->
                    _gateState.value = ProfileGateUiState.Error(
                        userRepository.mapExceptionToMessage(exception)
                    )
                }
        }
    }

    fun onNameChanged(value: String) {
        _name.update { value.take(MAX_NAME_LENGTH) }
        if (_setupState.value is ProfileSetupUiState.Error) {
            _setupState.value = ProfileSetupUiState.Idle
        }
    }

    fun saveProfile() {
        if (_setupState.value is ProfileSetupUiState.Loading) return

        val trimmedName = _name.value.trim()
        if (trimmedName.isBlank()) {
            _setupState.value = ProfileSetupUiState.Error("Please enter your name")
            return
        }

        viewModelScope.launch {
            _setupState.value = ProfileSetupUiState.Loading

            userRepository.createUserProfile(trimmedName)
                .onSuccess {
                    _setupState.value = ProfileSetupUiState.Success
                }
                .onFailure { exception ->
                    _setupState.value = ProfileSetupUiState.Error(
                        userRepository.mapExceptionToMessage(exception)
                    )
                }
        }
    }

    fun resetSetupState() {
        _setupState.value = ProfileSetupUiState.Idle
    }

    fun prepareForProfileCheck() {
        _gateState.value = ProfileGateUiState.Idle
    }

    fun resetForSignOut() {
        _gateState.value = ProfileGateUiState.Idle
        _setupState.value = ProfileSetupUiState.Idle
        _name.value = ""
    }

    companion object {
        private const val MAX_NAME_LENGTH = 50
    }
}
