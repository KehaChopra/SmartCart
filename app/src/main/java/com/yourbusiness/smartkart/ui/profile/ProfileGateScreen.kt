package com.yourbusiness.smartkart.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileGateScreen(
    viewModel: ProfileSetupViewModel,
    onProfileExists: () -> Unit,
    onNeedsProfileSetup: () -> Unit
) {
    val gateState by viewModel.gateState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (gateState is ProfileGateUiState.Idle) {
            viewModel.checkUserProfile()
        }
    }

    LaunchedEffect(gateState) {
        when (gateState) {
            ProfileGateUiState.ProfileExists -> onProfileExists()
            ProfileGateUiState.NeedsProfileSetup -> onNeedsProfileSetup()
            else -> Unit
        }
    }

    when (val state = gateState) {
        ProfileGateUiState.Idle,
        ProfileGateUiState.Loading,
        ProfileGateUiState.ProfileExists,
        ProfileGateUiState.NeedsProfileSetup -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading your profile…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is ProfileGateUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Could not load your profile",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = viewModel::checkUserProfile) {
                    Text("Try again")
                }
            }
        }
    }
}
