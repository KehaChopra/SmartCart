package com.yourbusiness.smartkart.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourbusiness.smartkart.ui.theme.SmartKartTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: PhoneAuthViewModel = viewModel()
) {
    val activity = LocalContext.current as ComponentActivity

    DisposableEffect(onAuthSuccess) {
        viewModel.onAuthSuccess = onAuthSuccess
        onDispose { viewModel.onAuthSuccess = null }
    }

    Scaffold(
        topBar = {
            if (viewModel.step == PhoneAuthStep.OTP) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = viewModel::goBackToPhoneNumber,
                            enabled = !viewModel.isVerifyingOtp && !viewModel.isResendingOtp
                        ) {
                            Text(
                                text = "←",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            when (viewModel.step) {
                PhoneAuthStep.PHONE_NUMBER -> {
                    PhoneNumberStep(
                        selectedCountry = viewModel.selectedCountry,
                        phoneNumber = viewModel.phoneNumber,
                        phoneError = viewModel.phoneError,
                        isLoading = viewModel.isSendingOtp,
                        onCountrySelected = viewModel::onCountrySelected,
                        onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                        onContinue = { viewModel.sendOtp(activity) }
                    )
                }

                PhoneAuthStep.OTP -> {
                    OtpStep(
                        phoneNumber = viewModel.formattedPhoneNumber(),
                        otpDigits = viewModel.otpDigits,
                        otpError = viewModel.otpError,
                        isVerifying = viewModel.isVerifyingOtp,
                        isResending = viewModel.isResendingOtp,
                        resendCountdown = viewModel.resendCountdown,
                        onOtpDigitChanged = viewModel::onOtpDigitChanged,
                        onOtpBackspace = viewModel::onOtpBackspace,
                        onVerify = viewModel::verifyOtp,
                        onResend = { viewModel.resendOtp(activity) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhoneAuthScreenPreview() {
    SmartKartTheme {
        PhoneAuthScreen(onAuthSuccess = {})
    }
}
