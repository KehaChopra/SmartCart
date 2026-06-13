package com.yourbusiness.smartkart.ui.cart

import java.util.Locale

fun formatRupee(amount: Double): String {
    return String.format(Locale("en", "IN"), "₹%.2f", amount)
}
