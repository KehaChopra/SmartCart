package com.yourbusiness.smartkart.data.model

data class ShoppingSession(
    val sessionId: String,
    val cartId: String,
    val userId: String,
    val items: List<SessionItem>,
    val totalAmount: Double,
    val status: String
)
