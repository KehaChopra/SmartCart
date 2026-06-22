package com.yourbusiness.smartkart.ui.auth

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.yourbusiness.smartkart.ui.auth.model.Country
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class PhoneAuthStep {
    PHONE_NUMBER,
    OTP
}

class PhoneAuthViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var verificationId: String? = null
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countdownJob: Job? = null

    var onAuthSuccess: (() -> Unit)? = null

    var step by mutableStateOf(PhoneAuthStep.PHONE_NUMBER)
        private set

    var selectedCountry by mutableStateOf(Country.default)
        private set

    var phoneNumber by mutableStateOf("")
        private set

    var otpDigits by mutableStateOf(List(6) { "" })
        private set

    var phoneError by mutableStateOf<String?>(null)
        private set

    var otpError by mutableStateOf<String?>(null)
        private set

    var isSendingOtp by mutableStateOf(false)
        private set

    var isVerifyingOtp by mutableStateOf(false)
        private set

    var isResendingOtp by mutableStateOf(false)
        private set

    var resendCountdown by mutableIntStateOf(0)
        private set

    fun onCountrySelected(country: Country) {
        selectedCountry = country
        phoneError = null
    }

    fun onPhoneNumberChanged(value: String) {
        phoneNumber = value.filter { it.isDigit() }.take(15)
        phoneError = null
    }

    fun onOtpDigitChanged(index: Int, digit: String) {
        val sanitized = digit.filter { it.isDigit() }.take(1)
        otpDigits = otpDigits.toMutableList().also { it[index] = sanitized }
        otpError = null
    }

    fun onOtpBackspace(index: Int) {
        otpDigits = otpDigits.toMutableList().also { it[index] = "" }
        otpError = null
    }

    fun sendOtp(activity: Activity) {
        if (isSendingOtp) return

        val validationError = validatePhoneNumber(phoneNumber)
        if (validationError != null) {
            phoneError = validationError
            return
        }

        isSendingOtp = true
        phoneError = null

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(buildE164Number())
            .setTimeout(PHONE_AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(createVerificationCallbacks(isResend = false))
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp() {
        if (isVerifyingOtp) return

        val enteredOtp = otpDigits.joinToString("")
        if (enteredOtp.length < OTP_LENGTH) {
            otpError = "Please enter the full 6-digit code"
            return
        }

        val currentVerificationId = verificationId
        if (currentVerificationId.isNullOrBlank()) {
            otpError = "Verification session expired. Please request a new code."
            return
        }

        isVerifyingOtp = true
        otpError = null

        val credential = PhoneAuthProvider.getCredential(currentVerificationId, enteredOtp)
        signInWithCredential(credential)
    }

    fun resendOtp(activity: Activity) {
        if (isResendingOtp || resendCountdown > 0) return

        val token = forceResendingToken
        if (token == null) {
            sendOtp(activity)
            return
        }

        isResendingOtp = true
        otpError = null

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(buildE164Number())
            .setTimeout(PHONE_AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(createVerificationCallbacks(isResend = true))
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun goBackToPhoneNumber() {
        step = PhoneAuthStep.PHONE_NUMBER
        otpError = null
        verificationId = null
        forceResendingToken = null
        clearOtp()
        countdownJob?.cancel()
        resendCountdown = 0
    }

    fun formattedPhoneNumber(): String = "${selectedCountry.dialCode} $phoneNumber"

    private fun createVerificationCallbacks(
        isResend: Boolean
    ): PhoneAuthProvider.OnVerificationStateChangedCallbacks {
        return object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (isResend) {
                    isResendingOtp = false
                } else {
                    isSendingOtp = false
                }
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("PHONE_AUTH_DEBUG", "Error class: ${e.javaClass.simpleName}")
                Log.e("PHONE_AUTH_DEBUG", "Error message: ${e.message}")
                if (e is FirebaseAuthException) {
                    Log.e("PHONE_AUTH_DEBUG", "Error code: ${e.errorCode}")
                }
                Log.e("PHONE_AUTH_DEBUG", "Full stack trace: ", e)

                val authErrorCode = (e as? FirebaseAuthException)?.errorCode
                Log.e(
                    TAG,
                    "Phone verification failed: errorCode=$authErrorCode, message=${e.message}",
                    e
                )
                if (isResend) {
                    isResendingOtp = false
                    otpError = mapFirebaseException(e, forOtpStep = true)
                } else {
                    isSendingOtp = false
                    phoneError = mapFirebaseException(e, forOtpStep = false)
                }
            }

            override fun onCodeSent(
                newVerificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = newVerificationId
                forceResendingToken = token

                if (isResend) {
                    isResendingOtp = false
                    clearOtp()
                    startResendCountdown()
                } else {
                    isSendingOtp = false
                    step = PhoneAuthStep.OTP
                    clearOtp()
                    startResendCountdown()
                }
            }
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                isVerifyingOtp = false
                isSendingOtp = false
                isResendingOtp = false

                if (task.isSuccessful) {
                    onAuthSuccess?.invoke()
                } else {
                    val message = mapVerificationFailure(task.exception)
                    if (step == PhoneAuthStep.OTP) {
                        otpError = message
                        clearOtp()
                    } else {
                        phoneError = message
                    }
                }
            }
    }

    private fun buildE164Number(): String {
        val dialCodeDigits = selectedCountry.dialCode.removePrefix("+")
        return "+$dialCodeDigits$phoneNumber"
    }

    private fun validatePhoneNumber(number: String): String? {
        return when {
            number.isBlank() -> "Phone number is required"
            number.length < 7 -> "Phone number is too short"
            number.length > 15 -> "Phone number is too long"
            else -> null
        }
    }

    private fun clearOtp() {
        otpDigits = List(OTP_LENGTH) { "" }
        otpError = null
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        resendCountdown = RESEND_COUNTDOWN_SECONDS
        countdownJob = viewModelScope.launch {
            while (resendCountdown > 0) {
                delay(1_000)
                resendCountdown--
            }
        }
    }

    private fun mapVerificationFailure(exception: Exception?): String {
        if (exception is FirebaseAuthException) {
            return when (exception.errorCode) {
                AuthError.INVALID_VERIFICATION_CODE,
                AuthError.INVALID_CREDENTIAL -> "Invalid OTP"
                else -> mapFirebaseException(exception, forOtpStep = true)
            }
        }
        return "Invalid OTP"
    }

    private fun mapFirebaseException(
        exception: FirebaseException,
        forOtpStep: Boolean
    ): String {
        val rawMessage = exception.localizedMessage.orEmpty()
        if (rawMessage.contains("app identifier", ignoreCase = true) ||
            rawMessage.contains("Play Integrity", ignoreCase = true) ||
            rawMessage.contains("reCAPTCHA", ignoreCase = true)
        ) {
            return buildAppIdentifierErrorMessage()
        }

        if (exception is FirebaseTooManyRequestsException) {
            return "Too many attempts. Firebase has temporarily blocked OTP requests. " +
                "Wait 1–2 hours, or use a test phone number in Firebase Console for development."
        }

        if (exception is FirebaseAuthException) {
            return when (exception.errorCode) {
                AuthError.INVALID_PHONE_NUMBER ->
                    "Invalid phone number. Please check and try again."

                AuthError.TOO_MANY_REQUESTS ->
                    "Too many OTP attempts. Wait 1–2 hours before trying again, " +
                        "or add a test phone number in Firebase Console → Authentication → Phone."

                AuthError.QUOTA_EXCEEDED ->
                    "Daily SMS limit reached (10/day on free plan). Try again tomorrow " +
                        "or add a billing account in Firebase to increase quota."

                AuthError.SESSION_EXPIRED ->
                    "Verification code expired. Please request a new one."

                AuthError.INVALID_VERIFICATION_CODE,
                AuthError.INVALID_CREDENTIAL ->
                    "Invalid OTP"

                AuthError.INVALID_VERIFICATION_ID ->
                    "Verification session expired. Please request a new code."

                AuthError.NETWORK_REQUEST_FAILED ->
                    "Network error. Check your connection and try again."

                AuthError.MISSING_VERIFICATION_CODE ->
                    if (forOtpStep) "Please enter the full 6-digit code"
                    else "Verification failed. Please try again."

                AuthError.MISSING_VERIFICATION_ID ->
                    "Verification session expired. Please request a new code."

                AuthError.CREDENTIAL_ALREADY_IN_USE ->
                    "This phone number is already linked to another account."

                AuthError.USER_DISABLED ->
                    "This account has been disabled. Contact support."

                AuthError.APP_NOT_AUTHORIZED ->
                    buildAppIdentifierErrorMessage()

                AuthError.OPERATION_NOT_ALLOWED ->
                    mapOperationNotAllowedMessage(exception)

                else -> exception.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Something went wrong. Please try again."
            }
        }

        return exception.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "Something went wrong. Please try again."
    }

    private fun mapOperationNotAllowedMessage(exception: FirebaseAuthException): String {
        val message = exception.localizedMessage.orEmpty()
        return when {
            message.contains("region", ignoreCase = true) ->
                "India (+91) is not enabled for SMS. In Firebase Console go to " +
                    "Authentication → Settings → SMS region policy → Allow → add India (IN). " +
                    "Real SMS also requires the Blaze plan. Use a test phone number while developing."

            else -> message.takeIf { it.isNotBlank() }
                ?: "Phone sign-in is not allowed for this Firebase project."
        }
    }

    private fun buildAppIdentifierErrorMessage(): String {
        return "Firebase could not verify this app. Uninstall SmartKart, then in Android Studio " +
            "run Build → Clean Project → Rebuild Project, and install the new APK. " +
            "If it still fails, confirm SHA-1/SHA-256 are in Firebase and google-services.json " +
            "has oauth_client entries."
    }

    private object AuthError {
        const val INVALID_PHONE_NUMBER = "ERROR_INVALID_PHONE_NUMBER"
        const val TOO_MANY_REQUESTS = "ERROR_TOO_MANY_REQUESTS"
        const val QUOTA_EXCEEDED = "ERROR_QUOTA_EXCEEDED"
        const val SESSION_EXPIRED = "ERROR_SESSION_EXPIRED"
        const val INVALID_VERIFICATION_CODE = "ERROR_INVALID_VERIFICATION_CODE"
        const val INVALID_CREDENTIAL = "ERROR_INVALID_CREDENTIAL"
        const val INVALID_VERIFICATION_ID = "ERROR_INVALID_VERIFICATION_ID"
        const val NETWORK_REQUEST_FAILED = "ERROR_NETWORK_REQUEST_FAILED"
        const val MISSING_VERIFICATION_CODE = "ERROR_MISSING_VERIFICATION_CODE"
        const val MISSING_VERIFICATION_ID = "ERROR_MISSING_VERIFICATION_ID"
        const val CREDENTIAL_ALREADY_IN_USE = "ERROR_CREDENTIAL_ALREADY_IN_USE"
        const val USER_DISABLED = "ERROR_USER_DISABLED"
        const val OPERATION_NOT_ALLOWED = "ERROR_OPERATION_NOT_ALLOWED"
        const val APP_NOT_AUTHORIZED = "ERROR_APP_NOT_AUTHORIZED"
    }

    companion object {
        private const val TAG = "PhoneAuth"
        private const val OTP_LENGTH = 6
        private const val PHONE_AUTH_TIMEOUT_SECONDS = 60L
        private const val RESEND_COUNTDOWN_SECONDS = 30
    }
}
