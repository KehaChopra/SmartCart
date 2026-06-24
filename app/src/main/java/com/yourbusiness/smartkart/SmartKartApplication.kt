package com.yourbusiness.smartkart

import android.app.Application
import com.razorpay.Checkout

class SmartKartApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Checkout.preload(applicationContext)
    }
}
