package com.yourbusiness.smartkart.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.yourbusiness.smartkart.data.model.SessionItem
import com.yourbusiness.smartkart.data.model.ShoppingSession
import kotlinx.coroutines.tasks.await

class SessionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private var listenerRegistration: ListenerRegistration? = null

    suspend fun getSessionIdForCart(cartId: String): Result<String> {
        return try {
            val snapshot = firestore.collection(CARTS_COLLECTION)
                .document(cartId)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.failure(IllegalStateException("Cart not found. Please scan a cart again."))
            }

            val sessionId = snapshot.getString(FIELD_CURRENT_SESSION_ID)?.trim()
            if (sessionId.isNullOrBlank()) {
                return Result.failure(
                    IllegalStateException("No active session for this cart. Please scan the cart QR code again.")
                )
            }

            Result.success(sessionId)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun observeSession(
        sessionId: String,
        onSessionUpdate: (Result<ShoppingSession>) -> Unit
    ) {
        removeListener()

        listenerRegistration = firestore.collection(SESSIONS_COLLECTION)
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onSessionUpdate(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    onSessionUpdate(
                        Result.failure(IllegalStateException("Shopping session not found."))
                    )
                    return@addSnapshotListener
                }

                runCatching { parseSession(snapshot) }
                    .fold(
                        onSuccess = { session -> onSessionUpdate(Result.success(session)) },
                        onFailure = { exception -> onSessionUpdate(Result.failure(exception)) }
                    )
            }
    }

    fun removeListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    fun mapExceptionToMessage(exception: Throwable): String {
        if (exception is IllegalStateException) {
            return exception.message ?: "Could not load your cart. Please try again."
        }

        if (exception is FirebaseFirestoreException) {
            return when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "You do not have permission to view this cart. Check Firestore security rules."

                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Could not reach the server. Check your internet connection."

                else -> exception.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Could not load your cart. Please try again."
            }
        }

        return exception.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "Could not load your cart. Please try again."
    }

    private fun parseSession(snapshot: DocumentSnapshot): ShoppingSession {
        val sessionId = snapshot.getString(FIELD_SESSION_ID) ?: snapshot.id
        val cartId = snapshot.getString(FIELD_CART_ID)
            ?: throw IllegalStateException("Session is missing cart information.")
        val userId = snapshot.getString(FIELD_USER_ID).orEmpty()
        val status = snapshot.getString(FIELD_STATUS).orEmpty()
        val totalAmount = snapshot.getDouble(FIELD_TOTAL_AMOUNT) ?: 0.0
        val items = parseItems(snapshot.get(FIELD_ITEMS))

        return ShoppingSession(
            sessionId = sessionId,
            cartId = cartId,
            userId = userId,
            items = items,
            totalAmount = totalAmount,
            status = status
        )
    }

    private fun parseItems(rawItems: Any?): List<SessionItem> {
        if (rawItems !is List<*>) return emptyList()

        return rawItems.mapNotNull { rawItem ->
            if (rawItem !is Map<*, *>) return@mapNotNull null

            val barcode = rawItem["barcode"]?.toString()?.trim().orEmpty()
            val name = rawItem["name"]?.toString()?.trim().orEmpty()
            if (barcode.isBlank() || name.isBlank()) return@mapNotNull null

            val price = when (val rawPrice = rawItem["price"]) {
                is Number -> rawPrice.toDouble()
                is String -> rawPrice.toDoubleOrNull()
                else -> null
            } ?: return@mapNotNull null

            val quantity = when (val rawQty = rawItem["qty"] ?: rawItem["quantity"]) {
                is Number -> rawQty.toInt()
                is String -> rawQty.toIntOrNull()
                else -> 1
            } ?: 1

            SessionItem(
                barcode = barcode,
                name = name,
                price = price,
                quantity = quantity
            )
        }
    }

    companion object {
        private const val CARTS_COLLECTION = "carts"
        private const val SESSIONS_COLLECTION = "sessions"
        private const val FIELD_CURRENT_SESSION_ID = "currentSessionId"
        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_CART_ID = "cartId"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_ITEMS = "items"
        private const val FIELD_TOTAL_AMOUNT = "totalAmount"
        private const val FIELD_STATUS = "status"
    }
}
