package com.yourbusiness.smartkart.data.model

data class SessionItem(
    val barcode: String,
    val name: String,
    val price: Double,
    val quantity: Int
) {
    val lineTotal: Double
        get() = price * quantity
}

fun List<SessionItem>.calculateTotalAmount(): Double = sumOf { it.lineTotal }
