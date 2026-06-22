package com.yourbusiness.smartkart.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun userProfileExists(uid: String): Result<Boolean> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
            Result.success(snapshot.exists())
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getActiveCart(uid: String): Result<String?> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.failure(
                    IllegalStateException("User profile not found. Please set up your profile again.")
                )
            }

            Result.success(parseActiveCartValue(snapshot.get(FIELD_ACTIVE_CART)))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun updateActiveCart(uid: String, cartId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(FIELD_ACTIVE_CART, cartId)
                .await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun clearActiveCart(uid: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update(FIELD_ACTIVE_CART, null)
                .await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun createUserProfile(name: String): Result<Unit> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("You are not signed in. Please log in again."))

        val uid = currentUser.uid
        val phone = currentUser.phoneNumber
            ?: return Result.failure(IllegalStateException("Phone number is missing. Please sign in again."))

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Name is required"))
        }

        val userData = mapOf(
            FIELD_UID to uid,
            FIELD_PHONE to phone,
            FIELD_NAME to trimmedName,
            FIELD_ACTIVE_CART to null,
            FIELD_CREATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        return try {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(userData)
                .await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun mapActiveCartUpdateError(exception: Throwable): String {
        if (exception is FirebaseFirestoreException &&
            exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        ) {
            return "Firestore permission denied when saving activeCart. Update your Firestore security rules."
        }
        return mapExceptionToMessage(exception)
    }

    fun mapExceptionToMessage(exception: Throwable): String {
        if (exception is IllegalArgumentException || exception is IllegalStateException) {
            return exception.message ?: "Something went wrong. Please try again."
        }

        if (exception is FirebaseFirestoreException) {
            return when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "You do not have permission to save your profile. Please contact support."

                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Could not reach the server. Check your internet connection and try again."

                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "The request took too long. Please try again."

                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Profile service is unavailable right now. Please try again later."

                else -> exception.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Could not save your profile. Please try again."
            }
        }

        val message = exception.localizedMessage?.lowercase().orEmpty()
        return when {
            message.contains("network") || message.contains("connection") ->
                "Network error. Check your connection and try again."

            else -> exception.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Something went wrong. Please try again."
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FIELD_UID = "uid"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_NAME = "name"
        private const val FIELD_ACTIVE_CART = "activeCart"
        private const val FIELD_CREATED_AT = "createdAt"

        fun parseActiveCartValue(rawValue: Any?): String? {
            if (rawValue == null) return null

            val normalized = when (rawValue) {
                is String -> rawValue.trim()
                else -> rawValue.toString().trim()
            }

            if (normalized.isBlank()) return null
            if (normalized.equals("null", ignoreCase = true)) return null

            return normalized
        }
    }
}
