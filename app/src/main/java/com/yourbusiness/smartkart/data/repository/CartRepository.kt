package com.yourbusiness.smartkart.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.yourbusiness.smartkart.data.CartBindConfig
import com.yourbusiness.smartkart.data.CartIdParser
import com.yourbusiness.smartkart.data.api.BindCartRequest
import com.yourbusiness.smartkart.data.api.BindCartResponse
import com.yourbusiness.smartkart.data.api.CartApiClient
import com.yourbusiness.smartkart.data.api.CartApiService
import com.yourbusiness.smartkart.data.api.CloudFunctionsApiClient
import com.yourbusiness.smartkart.data.api.CloudFunctionsApiService
import com.yourbusiness.smartkart.data.api.RemoveItemRequest
import retrofit2.Response
import java.io.IOException

class CartRepository(
    private val api: CartApiService = CartApiClient.service,
    private val cloudFunctionsApi: CloudFunctionsApiService = CloudFunctionsApiClient.service,
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val gson: Gson = Gson()
) {

    suspend fun removeItemFromCart(cartId: String, barcode: String): Result<Unit> {
        val trimmedBarcode = barcode.trim()
        if (trimmedBarcode.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid item. Please try again."))
        }

        return try {
            val response = cloudFunctionsApi.deleteItemFromCart(
                RemoveItemRequest(
                    cartId = cartId,
                    barcode = trimmedBarcode,
                    secret = CartBindConfig.cartSecret
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        IOException(body?.error ?: "Could not remove item. Please try again.")
                    )
                }
            } else {
                Result.failure(IOException(parseRemoveErrorMessage(response)))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun bindCartToUser(rawCartId: String): Result<String> {
        val trimmedCartId = CartIdParser.parse(rawCartId)
        if (trimmedCartId.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Invalid QR code. Please scan a cart QR code."))
        }

        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("You are not signed in. Please log in again."))

        return try {
            val response = api.bindCartToUser(
                BindCartRequest(
                    cartId = trimmedCartId,
                    userId = userId,
                    secret = CartBindConfig.cartSecret
                )
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    userRepository.updateActiveCart(userId, trimmedCartId)
                        .fold(
                            onSuccess = { Result.success(trimmedCartId) },
                            onFailure = { firestoreError ->
                                Result.failure(
                                    IOException(
                                        "Cart connected on server but could not save to your profile: " +
                                            userRepository.mapActiveCartUpdateError(firestoreError) +
                                            " (cart: $trimmedCartId)"
                                    )
                                )
                            }
                        )
                } else {
                    Result.failure(
                        IOException(body?.error ?: "Could not connect to this cart. Please try again.")
                    )
                }
            } else {
                Result.failure(IOException(parseErrorMessage(response)))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun mapExceptionToMessage(exception: Throwable): String {
        if (exception is IllegalArgumentException || exception is IllegalStateException) {
            return exception.message ?: "Something went wrong. Please try again."
        }

        if (exception is IOException) {
            return exception.message ?: "Network error. Check your connection and try again."
        }

        return userRepository.mapExceptionToMessage(exception)
    }

    private fun parseRemoveErrorMessage(response: Response<BindCartResponse>): String {
        val errorBody = response.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            runCatching {
                gson.fromJson(errorBody, BindCartResponse::class.java)?.error
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        }

        return when (response.code()) {
            403 -> "Unauthorized request. Please update the app and try again."
            404 -> "Item not found in cart."
            400 -> "No active shopping session. Please scan your cart again."
            500 -> "Server error. Please try again in a moment."
            else -> "Could not remove item. Please try again."
        }
    }

    private fun parseErrorMessage(response: Response<BindCartResponse>): String {
        val errorBody = response.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            runCatching {
                gson.fromJson(errorBody, BindCartResponse::class.java)?.error
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
        }

        return when (response.code()) {
            403 -> "Unauthorized request. Please update the app and try again."
            404 -> "Cart not found. Please scan a valid cart QR code."
            409 -> "This cart is already in use. Try another cart."
            500 -> "Server error. Make sure the cart exists in Firestore and is not already in use."
            else -> "Could not connect to this cart. Please try again."
        }
    }

    fun formatBindError(message: String, cartId: String): String {
        return "$message\n\nScanned cart ID: $cartId"
    }
}
