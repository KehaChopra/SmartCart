package com.yourbusiness.smartkart.ui.profile

sealed class ProfileGateUiState {
    data object Idle : ProfileGateUiState()
    data object Loading : ProfileGateUiState()
    data object ProfileExists : ProfileGateUiState()
    data object NeedsProfileSetup : ProfileGateUiState()
    data class Error(val message: String) : ProfileGateUiState()
}

sealed class ProfileSetupUiState {
    data object Idle : ProfileSetupUiState()
    data object Loading : ProfileSetupUiState()
    data object Success : ProfileSetupUiState()
    data class Error(val message: String) : ProfileSetupUiState()
}
