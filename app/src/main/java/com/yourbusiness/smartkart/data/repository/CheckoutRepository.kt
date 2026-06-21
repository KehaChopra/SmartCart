package com.yourbusiness.smartkart.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class CheckoutRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun completeCheckout(cartId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("You are not signed in. Please log in again."))

        return try {
            val cartSnapshot = firestore.collection(CARTS_COLLECTION)
                .document(cartId)
                .get()
                .await()

            if (!cartSnapshot.exists()) {
                return Result.failure(IllegalStateException("Cart not found."))
            }

            val sessionId = cartSnapshot.getString(FIELD_CURRENT_SESSION_ID)?.trim()
            if (sessionId.isNullOrBlank()) {
                return Result.failure(IllegalStateException("No active session for this cart."))
            }

            val batch = firestore.batch()

            val userRef = firestore.collection(USERS_COLLECTION).document(uid)
            batch.update(userRef, FIELD_ACTIVE_CART, null)

            val sessionRef = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
            batch.update(sessionRef, FIELD_STATUS, SESSION_STATUS_COMPLETED)

            val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
            batch.update(
                cartRef,
                mapOf(
                    FIELD_STATUS to CART_STATUS_AVAILABLE,
                    FIELD_CURRENT_SESSION_ID to FieldValue.delete()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun mapExceptionToMessage(exception: Throwable): String {
        if (exception is IllegalStateException) {
            return exception.message ?: "Could not complete checkout. Please try again."
        }

        if (exception is FirebaseFirestoreException) {
            return when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "You do not have permission to complete checkout. Check Firestore rules."

                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Could not reach the server. Check your internet connection."

                else -> exception.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Could not complete checkout. Please try again."
            }
        }

        return exception.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "Could not complete checkout. Please try again."
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val CARTS_COLLECTION = "carts"
        private const val SESSIONS_COLLECTION = "sessions"
        private const val FIELD_ACTIVE_CART = "activeCart"
        private const val FIELD_STATUS = "status"
        private const val FIELD_CURRENT_SESSION_ID = "currentSessionId"
        private const val CART_STATUS_AVAILABLE = "available"
        private const val SESSION_STATUS_COMPLETED = "completed"
    }
}
