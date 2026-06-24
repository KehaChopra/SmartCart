package com.yourbusiness.smartkart.data.api

data class CreateOrderResponse(
    val success: Boolean? = null,
    val orderId: String? = null,
    val amountPaise: Int? = null,
    val currency: String? = null,
    val razorpayKeyId: String? = null,
    val error: String? = null
)
