package com.yourbusiness.smartkart.ui.checkout

data class PaymentSdkConfig(
    val orderId: String,
    val amountPaise: Int,
    val currency: String,
    val razorpayKeyId: String,
    val userPhone: String
)
