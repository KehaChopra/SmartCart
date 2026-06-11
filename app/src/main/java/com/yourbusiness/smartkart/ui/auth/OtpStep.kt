package com.yourbusiness.smartkart.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourbusiness.smartkart.ui.auth.components.OtpInputField

@Composable
fun OtpStep(
    phoneNumber: String,
    otpDigits: List<String>,
    otpError: String?,
    isVerifying: Boolean,
    isResending: Boolean,
    resendCountdown: Int,
    onOtpDigitChanged: (index: Int, digit: String) -> Unit,
    onOtpBackspace: (index: Int) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = isVerifying || isResending

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Enter verification code",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sent to $phoneNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OtpInputField(
            otpDigits = otpDigits,
            onDigitChange = onOtpDigitChanged,
            onBackspace = onOtpBackspace,
            isError = otpError != null,
            enabled = !isBusy
        )

        if (otpError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = otpError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isBusy
        ) {
            if (isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Verify")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onResend,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = !isBusy && resendCountdown == 0
        ) {
            when {
                isResending -> {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                resendCountdown > 0 -> {
                    Text("Resend code in ${resendCountdown}s")
                }
                else -> {
                    Text("Resend code")
                }
            }
        }
    }
}
