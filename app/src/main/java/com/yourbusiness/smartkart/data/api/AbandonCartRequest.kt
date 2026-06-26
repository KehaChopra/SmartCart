package com.yourbusiness.smartkart.data.api

data class AbandonCartRequest(
    val cartId: String,
    val sessionId: String,
    val userId: String,
    val secret: String
)
