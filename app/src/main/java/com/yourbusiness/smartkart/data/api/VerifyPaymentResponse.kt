package com.yourbusiness.smartkart.data.api

data class VerifyPaymentResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val sessionId: String? = null,
    val cartId: String? = null,
    val reason: String? = null,
    val error: String? = null
)
