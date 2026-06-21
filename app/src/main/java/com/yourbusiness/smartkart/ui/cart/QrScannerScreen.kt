package com.yourbusiness.smartkart.ui.cart

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourbusiness.smartkart.ui.cart.components.BarcodeCameraPreview
import com.yourbusiness.smartkart.ui.cart.components.QrViewfinderOverlay
import com.yourbusiness.smartkart.ui.cart.components.SmartKartScannerBrandHeader
import com.yourbusiness.smartkart.ui.theme.SmartKartBackground
import com.yourbusiness.smartkart.ui.theme.SmartKartGreen
import com.yourbusiness.smartkart.ui.theme.SmartKartScannerBackground
import com.yourbusiness.smartkart.ui.theme.SmartKartScannerButton
import com.yourbusiness.smartkart.ui.theme.SmartKartScannerSurface
import com.yourbusiness.smartkart.ui.theme.SmartKartScannerTextSecondary
import kotlinx.coroutines.delay

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
    var torchEnabled by remember { mutableStateOf(false) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = SmartKartScannerBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

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
        containerColor = SmartKartScannerBackground,
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
                ScannerContent(
                    uiState = uiState,
                    isCameraScanningEnabled = viewModel.isCameraScanningEnabled,
                    torchEnabled = torchEnabled,
                    onTorchToggle = { torchEnabled = !torchEnabled },
                    onBarcodeDetected = viewModel::onBarcodeDetected,
                    onSimulateScan = { viewModel.onBarcodeDetected(SIMULATE_CART_ID) },
                    onRetry = viewModel::retryScanning,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ScannerContent(
    uiState: QrScannerUiState,
    isCameraScanningEnabled: Boolean,
    torchEnabled: Boolean,
    onTorchToggle: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onSimulateScan: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showCamera = uiState is QrScannerUiState.Scanning || uiState is QrScannerUiState.Loading
    val statusText = when (uiState) {
        QrScannerUiState.Scanning -> "Looking for a cart QR code..."
        QrScannerUiState.Loading -> "Connecting to cart..."
        is QrScannerUiState.Error -> uiState.message
        is QrScannerUiState.Success -> "Cart connected! Opening your cart…"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmartKartScannerBrandHeader()

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Scan Cart QR Code",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SmartKartBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Point your camera at the QR code on the cart",
            style = MaterialTheme.typography.bodyLarge,
            color = SmartKartScannerTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (showCamera) {
                BarcodeCameraPreview(
                    isScanningEnabled = isCameraScanningEnabled,
                    onBarcodeDetected = onBarcodeDetected,
                    torchEnabled = torchEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SmartKartScannerSurface)
                )
            }

            QrViewfinderOverlay(
                isAnimating = uiState is QrScannerUiState.Scanning,
                modifier = Modifier.fillMaxSize()
            )

            when (uiState) {
                QrScannerUiState.Loading -> {
                    ViewfinderLoadingOverlay()
                }

                is QrScannerUiState.Error -> {
                    ViewfinderErrorOverlay()
                }

                is QrScannerUiState.Success -> {
                    ViewfinderLoadingOverlay(message = "Cart connected!")
                }

                QrScannerUiState.Scanning -> Unit
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        IconButton(
            onClick = onTorchToggle,
            enabled = showCamera,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SmartKartScannerSurface)
        ) {
            Icon(
                imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = if (torchEnabled) "Turn flash off" else "Turn flash on",
                tint = SmartKartBackground,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = SmartKartScannerTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState is QrScannerUiState.Scanning) {
            Button(
                onClick = onSimulateScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartKartScannerButton,
                    contentColor = SmartKartBackground
                )
            ) {
                Text(
                    text = "Simulate Scan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp)
                )
            }
        } else if (uiState is QrScannerUiState.Error) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartKartScannerButton,
                    contentColor = SmartKartBackground
                )
            ) {
                Text(
                    text = "Scan again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

@Composable
private fun ViewfinderLoadingOverlay(
    message: String = "Connecting to cart…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartKartScannerBackground.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = SmartKartGreen,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SmartKartBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ViewfinderErrorOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartKartScannerBackground.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Could not connect",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SmartKartBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try scanning again",
                style = MaterialTheme.typography.bodySmall,
                color = SmartKartScannerTextSecondary,
                textAlign = TextAlign.Center
            )
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
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmartKartScannerBrandHeader()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Camera permission required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SmartKartBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SmartKart needs camera access to scan cart QR codes. " +
                "Please allow camera permission to continue.",
            style = MaterialTheme.typography.bodyLarge,
            color = SmartKartScannerTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SmartKartGreen,
                contentColor = SmartKartBackground
            )
        ) {
            Text(
                text = "Allow camera access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SmartKartScannerButton,
                contentColor = SmartKartBackground
            )
        ) {
            Text(
                text = "Sign out",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private const val SUCCESS_MESSAGE_DELAY_MS = 1_500L
private const val SIMULATE_CART_ID = "CART_001"
