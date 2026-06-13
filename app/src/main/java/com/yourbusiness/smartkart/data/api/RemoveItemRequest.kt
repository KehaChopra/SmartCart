package com.yourbusiness.smartkart.data.api

data class RemoveItemRequest(
    val cartId: String,
    val barcode: String,
    val secret: String
)
