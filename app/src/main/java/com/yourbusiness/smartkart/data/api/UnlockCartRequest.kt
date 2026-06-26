package com.yourbusiness.smartkart.data.api

data class UnlockCartRequest(
    val cartId: String,
    val secret: String
)
