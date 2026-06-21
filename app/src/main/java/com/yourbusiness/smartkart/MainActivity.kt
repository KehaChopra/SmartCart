package com.yourbusiness.smartkart

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.yourbusiness.smartkart.data.model.SessionItem
import com.yourbusiness.smartkart.data.repository.UserRepository
import com.yourbusiness.smartkart.ui.auth.PhoneAuthScreen
import com.yourbusiness.smartkart.ui.cart.CartGateScreen
import com.yourbusiness.smartkart.ui.cart.CartCheckViewModel
import com.yourbusiness.smartkart.ui.cart.CartScreen
import com.yourbusiness.smartkart.ui.cart.QrScannerScreen
import com.yourbusiness.smartkart.ui.checkout.CheckoutScreen
import com.yourbusiness.smartkart.ui.checkout.CheckoutSuccessScreen
import com.yourbusiness.smartkart.ui.profile.ProfileGateScreen
import com.yourbusiness.smartkart.ui.profile.ProfileSetupScreen
import com.yourbusiness.smartkart.ui.profile.ProfileSetupViewModel
import com.yourbusiness.smartkart.ui.theme.SmartKartTheme

private enum class AppDestination {
    AUTH,
    PROFILE_GATE,
    PROFILE_SETUP,
    CART_GATE,
    QR_SCANNER,
    CART,
    CHECKOUT,
    CHECKOUT_SUCCESS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logFirebaseConfig()
        enableEdgeToEdge()
        setContent {
            SmartKartTheme {
                SmartKartApp()
            }
        }
    }

    private fun logFirebaseConfig() {
        val options = FirebaseApp.getInstance().options
        Log.d(
            TAG,
            "Firebase projectId=${options.projectId}, appId=${options.applicationId}"
        )
    }

    companion object {
        private const val TAG = "SmartKartFirebase"
    }
}

@Composable
private fun SmartKartApp(
    profileViewModel: ProfileSetupViewModel = viewModel(),
    cartCheckViewModel: CartCheckViewModel = viewModel()
) {
    val firebaseAuth = remember { FirebaseAuth.getInstance() }

    var destination by remember {
        mutableStateOf(
            if (firebaseAuth.currentUser != null) {
                AppDestination.PROFILE_GATE
            } else {
                AppDestination.AUTH
            }
        )
    }

    var activeCartId by remember { mutableStateOf<String?>(null) }
    var checkoutTotal by remember { mutableStateOf(0.0) }
    var checkoutItems by remember { mutableStateOf<List<SessionItem>>(emptyList()) }

    fun signOut() {
        firebaseAuth.signOut()
        profileViewModel.resetForSignOut()
        cartCheckViewModel.resetForSignOut()
        activeCartId = null
        checkoutTotal = 0.0
        checkoutItems = emptyList()
        destination = AppDestination.AUTH
    }

    fun goToCartCheck() {
        cartCheckViewModel.prepareForCartCheck()
        destination = AppDestination.CART_GATE
    }

    when (destination) {
        AppDestination.AUTH -> {
            PhoneAuthScreen(
                onAuthSuccess = {
                    profileViewModel.prepareForProfileCheck()
                    destination = AppDestination.PROFILE_GATE
                }
            )
        }

        AppDestination.PROFILE_GATE -> {
            ProfileGateScreen(
                viewModel = profileViewModel,
                onProfileExists = { goToCartCheck() },
                onNeedsProfileSetup = { destination = AppDestination.PROFILE_SETUP }
            )
        }

        AppDestination.PROFILE_SETUP -> {
            ProfileSetupScreen(
                viewModel = profileViewModel,
                onProfileComplete = { goToCartCheck() }
            )
        }

        AppDestination.CART_GATE -> {
            CartGateScreen(
                viewModel = cartCheckViewModel,
                onNavigateToScanner = { destination = AppDestination.QR_SCANNER },
                onNavigateToCart = { cartId ->
                    val validCartId = UserRepository.parseActiveCartValue(cartId)
                    if (validCartId == null) {
                        destination = AppDestination.QR_SCANNER
                    } else {
                        activeCartId = validCartId
                        destination = AppDestination.CART
                    }
                }
            )
        }

        AppDestination.QR_SCANNER -> {
            QrScannerScreen(
                onCartBound = { cartId ->
                    activeCartId = cartId
                    destination = AppDestination.CART
                },
                onSignOut = ::signOut
            )
        }

        AppDestination.CART -> {
            val cartId = UserRepository.parseActiveCartValue(activeCartId)
            if (cartId != null) {
                CartScreen(
                    cartId = cartId,
                    onCheckout = { total, items ->
                        checkoutTotal = total
                        checkoutItems = items
                        destination = AppDestination.CHECKOUT
                    },
                    onSignOut = ::signOut
                )
            } else {
                destination = AppDestination.QR_SCANNER
            }
        }

        AppDestination.CHECKOUT -> {
            val cartId = UserRepository.parseActiveCartValue(activeCartId)
            if (cartId != null) {
                CheckoutScreen(
                    cartId = cartId,
                    totalAmount = checkoutTotal,
                    items = checkoutItems,
                    onPaymentSuccess = { total ->
                        checkoutTotal = total
                        destination = AppDestination.CHECKOUT_SUCCESS
                    },
                    onBack = { destination = AppDestination.CART }
                )
            } else {
                destination = AppDestination.QR_SCANNER
            }
        }

        AppDestination.CHECKOUT_SUCCESS -> {
            CheckoutSuccessScreen(
                cartId = UserRepository.parseActiveCartValue(activeCartId).orEmpty(),
                totalAmount = checkoutTotal,
                onDone = {
                    activeCartId = null
                    checkoutItems = emptyList()
                    goToCartCheck()
                }
            )
        }
    }
}
