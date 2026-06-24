package com.yourbusiness.smartkart.data.repository

import com.google.gson.Gson
import com.yourbusiness.smartkart.data.CartBindConfig
import com.yourbusiness.smartkart.data.api.CloudFunctionsApiClient
import com.yourbusiness.smartkart.data.api.CloudFunctionsApiService
import com.yourbusiness.smartkart.data.api.CreateOrderRequest
import com.yourbusiness.smartkart.data.api.CreateOrderResponse
import com.yourbusiness.smartkart.data.api.VerifyPaymentRequest
import com.yourbusiness.smartkart.data.api.VerifyPaymentResponse
import com.yourbusiness.smartkart.ui.checkout.PaymentSdkConfig
import retrofit2.Response
import java.io.IOException

class PaymentRepository(
    private val api: CloudFunctionsApiService = CloudFunctionsApiClient.service,
    private val gson: Gson = Gson()
) {

    suspend fun createOrder(
        cartId: String,
        sessionId: String,
        userPhone: String
    ): Result<PaymentSdkConfig> {
        return try {
            val response = api.createOrder(
                CreateOrderRequest(
                    cartId = cartId,
                    sessionId = sessionId,
                    secret = CartBindConfig.cartSecret
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val orderId = body.orderId
                    val amountPaise = body.amountPaise
                    val currency = body.currency
                    val razorpayKeyId = body.razorpayKeyId

                    if (
                        orderId.isNullOrBlank() ||
                        amountPaise == null ||
                        amountPaise <= 0 ||
                        currency.isNullOrBlank() ||
                        razorpayKeyId.isNullOrBlank()
                    ) {
                        Result.failure(
                            IOException("Invalid payment order response. Please try again.")
                        )
                    } else {
                        Result.success(
                            PaymentSdkConfig(
                                orderId = orderId,
                                amountPaise = amountPaise,
                                currency = currency,
                                razorpayKeyId = razorpayKeyId,
                                userPhone = userPhone
                            )
                        )
                    }
                } else {
                    Result.failure(
                        IOException(body?.error ?: "Could not start payment. Please try again.")
                    )
                }
            } else {
                Result.failure(IOException(parseCreateOrderError(response)))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun verifyPayment(
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<Unit> {
        return try {
            val response = api.verifyPayment(
                VerifyPaymentRequest(
                    orderId = orderId,
                    paymentId = paymentId,
                    signature = signature,
                    secret = CartBindConfig.cartSecret
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.status == "paid" || (body?.success == true && body.status == "paid")) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        IOException(
                            body?.reason ?: body?.error ?: "Payment verification failed."
                        )
                    )
                }
            } else {
                Result.failure(IOException(parseVerifyPaymentError(response)))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun mapExceptionToMessage(exception: Throwable): String {
        if (exception is IOException) {
            return exception.message ?: "Network error. Check your connection and try again."
        }

        return exception.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "Something went wrong. Please try again."
    }

    private fun parseCreateOrderError(response: Response<CreateOrderResponse>): String {
        val errorBody = response.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            runCatching {
                gson.fromJson(errorBody, CreateOrderResponse::class.java)?.error
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        }

        return when (response.code()) {
            400 -> "Cannot start payment for this cart. Please try again."
            403 -> "Unauthorized request. Please update the app and try again."
            404 -> "Shopping session not found. Please scan your cart again."
            500 -> "Payment service is unavailable. Please try again."
            else -> "Could not start payment. Please try again."
        }
    }

    private fun parseVerifyPaymentError(response: Response<VerifyPaymentResponse>): String {
        val errorBody = response.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            runCatching {
                gson.fromJson(errorBody, VerifyPaymentResponse::class.java)
            }.getOrNull()?.let { body ->
                body.reason?.takeIf { it.isNotBlank() }?.let { return it }
                body.error?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }

        return when (response.code()) {
            400 -> "Invalid payment details. Please try again."
            403 -> "Unauthorized request. Please update the app and try again."
            500 -> "Could not verify payment. Please contact support."
            else -> "Could not verify payment. Please try again."
        }
    }
}
