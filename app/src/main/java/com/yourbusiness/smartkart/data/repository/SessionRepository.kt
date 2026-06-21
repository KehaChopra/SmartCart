package com.yourbusiness.smartkart.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.yourbusiness.smartkart.data.model.SessionItem
import com.yourbusiness.smartkart.data.model.ShoppingSession
import kotlinx.coroutines.tasks.await

class SessionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private var cartListenerRegistration: ListenerRegistration? = null
    private var sessionListenerRegistration: ListenerRegistration? = null
    private var observedSessionId: String? = null

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

    /**
     * Listens to the cart document for [currentSessionId], then attaches a real-time
     * listener on that session document's [items] field.
     */
    fun observeCartSession(
        cartId: String,
        onSessionUpdate: (Result<ShoppingSession>) -> Unit
    ) {
        removeListener()
        observedSessionId = null

        cartListenerRegistration = firestore.collection(CARTS_COLLECTION)
            .document(cartId)
            .addSnapshotListener(MetadataChanges.EXCLUDE) { cartSnapshot, error ->
                if (error != null) {
                    onSessionUpdate(Result.failure(error))
                    return@addSnapshotListener
                }

                if (cartSnapshot == null || !cartSnapshot.exists()) {
                    onSessionUpdate(
                        Result.failure(IllegalStateException("Cart not found. Please scan a cart again."))
                    )
                    return@addSnapshotListener
                }

                val sessionId = cartSnapshot.getString(FIELD_CURRENT_SESSION_ID)?.trim()
                if (sessionId.isNullOrBlank()) {
                    removeSessionListener()
                    observedSessionId = null
                    onSessionUpdate(
                        Result.failure(
                            IllegalStateException("No active session for this cart. Please scan the cart QR code again.")
                        )
                    )
                    return@addSnapshotListener
                }

                if (sessionId != observedSessionId) {
                    Log.d(TAG, "Listening to session $sessionId for cart $cartId")
                    observedSessionId = sessionId
                    attachSessionListener(sessionId, onSessionUpdate)
                }
            }
    }

    fun observeSession(
        sessionId: String,
        onSessionUpdate: (Result<ShoppingSession>) -> Unit
    ) {
        removeListener()
        observedSessionId = sessionId
        attachSessionListener(sessionId, onSessionUpdate)
    }

    private fun attachSessionListener(
        sessionId: String,
        onSessionUpdate: (Result<ShoppingSession>) -> Unit
    ) {
        removeSessionListener()

        sessionListenerRegistration = firestore.collection(SESSIONS_COLLECTION)
            .document(sessionId)
            .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Session listener error for $sessionId", error)
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
                        onSuccess = { session ->
                            Log.d(
                                TAG,
                                "Session $sessionId updated: ${session.items.size} item(s) from server"
                            )
                            onSessionUpdate(Result.success(session))
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to parse session $sessionId", exception)
                            onSessionUpdate(Result.failure(exception))
                        }
                    )
            }
    }

    fun removeListener() {
        cartListenerRegistration?.remove()
        cartListenerRegistration = null
        removeSessionListener()
        observedSessionId = null
    }

    private fun removeSessionListener() {
        sessionListenerRegistration?.remove()
        sessionListenerRegistration = null
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
        val rawItems = snapshot.get(FIELD_ITEMS)
        val items = parseItems(rawItems)

        if (rawItems != null && items.isEmpty()) {
            Log.w(
                TAG,
                "Session $sessionId has items field (${rawItems::class.java.simpleName}) but parsed 0 items. " +
                    "Ensure items is an array of maps with barcode, name, price (or cost), and quantity."
            )
        }

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
        val rawList = normalizeItemsRaw(rawItems) ?: return emptyList()

        return rawList.mapNotNull { rawItem ->
            if (rawItem !is Map<*, *>) {
                Log.w(TAG, "Skipping non-map item entry: ${rawItem?.javaClass?.simpleName}")
                return@mapNotNull null
            }

            val barcode = sequenceOf("barcode", "productBarcode", "sku")
                .mapNotNull { key -> rawItem[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
                .firstOrNull()
                ?: return@mapNotNull null

            val name = sequenceOf("name", "itemName", "productName", "title")
                .mapNotNull { key -> rawItem[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
                .firstOrNull()
                ?: return@mapNotNull null

            val price = sequenceOf("price", "cost", "unitPrice", "amount")
                .mapNotNull { key ->
                    when (val rawPrice = rawItem[key]) {
                        is Number -> rawPrice.toDouble()
                        is String -> rawPrice.toDoubleOrNull()
                        else -> null
                    }
                }
                .firstOrNull()
                ?: run {
                    Log.w(TAG, "Skipping item $barcode ($name): missing or invalid price")
                    return@mapNotNull null
                }

            val quantity = sequenceOf("qty", "quantity", "count")
                .mapNotNull { key ->
                    when (val rawQty = rawItem[key]) {
                        is Number -> rawQty.toInt()
                        is String -> rawQty.toIntOrNull()
                        else -> null
                    }
                }
                .firstOrNull()
                ?: 1

            SessionItem(
                barcode = barcode,
                name = name,
                price = price,
                quantity = quantity
            )
        }
    }

    private fun normalizeItemsRaw(rawItems: Any?): List<*>? {
        return when (rawItems) {
            null -> null
            is List<*> -> rawItems
            is Map<*, *> -> {
                Log.w(
                    TAG,
                    "items is stored as a map, not an array. Converting for display. " +
                        "In Firestore, set items as an array type."
                )
                rawItems.entries
                    .sortedBy { (key, _) ->
                        when (key) {
                            is Number -> key.toInt()
                            is String -> key.toIntOrNull() ?: Int.MAX_VALUE
                            else -> Int.MAX_VALUE
                        }
                    }
                    .mapNotNull { it.value }
            }
            else -> {
                Log.w(TAG, "items field has unexpected type: ${rawItems::class.java.name}")
                null
            }
        }
    }

    companion object {
        private const val TAG = "SessionRepository"
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
