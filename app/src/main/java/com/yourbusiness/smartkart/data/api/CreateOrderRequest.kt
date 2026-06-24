package com.yourbusiness.smartkart.data.api

data class CreateOrderRequest(
    val cartId: String,
    val sessionId: String,
    val secret: String
)
