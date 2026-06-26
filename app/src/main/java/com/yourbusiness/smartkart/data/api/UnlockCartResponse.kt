package com.yourbusiness.smartkart.data.api

data class UnlockCartResponse(
    val success: Boolean? = null,
    val unlocked: Boolean? = null,
    val cartId: String? = null,
    val error: String? = null
)
