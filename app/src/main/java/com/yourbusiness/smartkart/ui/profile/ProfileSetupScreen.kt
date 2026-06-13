package com.yourbusiness.smartkart.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourbusiness.smartkart.ui.theme.SmartKartTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onProfileComplete: () -> Unit,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val setupState by viewModel.setupState.collectAsStateWithLifecycle()

    val isLoading = setupState is ProfileSetupUiState.Loading
    val errorMessage = (setupState as? ProfileSetupUiState.Error)?.message

    LaunchedEffect(setupState) {
        if (setupState is ProfileSetupUiState.Success) {
            onProfileComplete()
            viewModel.resetSetupState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Set up your profile") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .imePadding()
        ) {
            Text(
                text = "What should we call you?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your name so we can personalize your experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = viewModel::onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your name") },
                placeholder = { Text("e.g. Alex") },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { error ->
                    { Text(error) }
                },
                enabled = !isLoading,
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = viewModel::saveProfile,
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
                    Text("Save")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSetupScreenPreview() {
    SmartKartTheme {
        ProfileSetupScreen(onProfileComplete = {})
    }
}
