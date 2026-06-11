package com.yourbusiness.smartkart.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yourbusiness.smartkart.ui.auth.components.CountryCodeSelector
import com.yourbusiness.smartkart.ui.auth.model.Country

@Composable
fun PhoneNumberStep(
    selectedCountry: Country,
    phoneNumber: String,
    phoneError: String?,
    isLoading: Boolean,
    onCountrySelected: (Country) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Text(
            text = "Enter your phone number",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll send you a verification code",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            CountryCodeSelector(
                selectedCountry = selectedCountry,
                onCountrySelected = onCountrySelected,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChanged,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = { Text("Phone number") },
                placeholder = { Text("1234567890") },
                isError = phoneError != null,
                supportingText = phoneError?.let { error ->
                    { Text(error) }
                },
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Continue")
            }
        }
    }
}
