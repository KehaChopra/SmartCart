package com.yourbusiness.smartkart.data.api

data class VerifyPaymentRequest(
    val orderId: String,
    val paymentId: String,
    val signature: String,
    val secret: String
)
