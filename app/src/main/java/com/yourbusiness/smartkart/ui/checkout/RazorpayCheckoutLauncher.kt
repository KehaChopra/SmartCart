package com.yourbusiness.smartkart.ui.checkout

import android.app.Activity
import com.razorpay.Checkout
import org.json.JSONObject

fun openRazorpayCheckout(activity: Activity, config: PaymentSdkConfig) {
    val checkout = Checkout()
    checkout.setKeyID(config.razorpayKeyId)

    val options = JSONObject().apply {
        put("name", "SmartKart")
        put("description", "Cart payment")
        put("order_id", config.orderId)
        put("currency", config.currency)
        put("amount", config.amountPaise)
        put("prefill", JSONObject().apply {
            put("contact", config.userPhone)
        })
        put("retry", JSONObject().apply {
            put("enabled", false)
        })
    }

    checkout.open(activity, options)
}
