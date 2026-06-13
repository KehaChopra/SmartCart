package com.yourbusiness.smartkart.data.api

data class BindCartRequest(
    val cartId: String,
    val userId: String,
    val secret: String
)
