package com.yourbusiness.smartkart.ui.cart

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourbusiness.smartkart.ui.cart.components.BarcodeCameraPreview
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onCartBound: (cartId: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QrScannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is QrScannerUiState.Success) {
            val cartId = (uiState as QrScannerUiState.Success).cartId
            snackbarHostState.showSnackbar("Cart $cartId connected!")
            delay(SUCCESS_MESSAGE_DELAY_MS)
            onCartBound(cartId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Scan cart QR code") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        when {
            !hasCameraPermission -> {
                CameraPermissionContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onSignOut = onSignOut
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (uiState is QrScannerUiState.Scanning || uiState is QrScannerUiState.Loading) {
                        BarcodeCameraPreview(
                            isScanningEnabled = viewModel.isCameraScanningEnabled,
                            onBarcodeDetected = viewModel::onBarcodeDetected,
                            modifier = Modifier.fillMaxSize()
                        )

                        ScannerOverlayHint(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(24.dp)
                        )
                    }

                    when (val state = uiState) {
                        QrScannerUiState.Scanning -> Unit

                        QrScannerUiState.Loading -> {
                            LoadingOverlay()
                        }

                        is QrScannerUiState.Error -> {
                            ErrorOverlay(
                                message = state.message,
                                onRetry = viewModel::retryScanning
                            )
                        }

                        is QrScannerUiState.Success -> {
                            LoadingOverlay(
                                message = "Cart connected! Opening your cart…"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionContent(
    onRequestPermission: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera permission required",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SmartKart needs camera access to scan cart QR codes. " +
                "Please allow camera permission to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow camera access")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onSignOut) {
            Text("Sign out")
        }
    }
}

@Composable
private fun ScannerOverlayHint(modifier: Modifier = Modifier) {
    Text(
        text = "Point your camera at the cart QR code",
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(16.dp)
    )
}

@Composable
private fun LoadingOverlay(
    message: String = "Connecting to cart…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Could not connect to cart",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRetry) {
                Text("Scan again")
            }
        }
    }
}

private const val SUCCESS_MESSAGE_DELAY_MS = 1_500L
