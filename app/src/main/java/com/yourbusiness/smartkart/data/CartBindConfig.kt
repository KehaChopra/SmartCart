package com.yourbusiness.smartkart.data

import com.yourbusiness.smartkart.BuildConfig

/**
 * Reads API secrets from [BuildConfig], which Gradle fills from local.properties.
 * Never hardcode secrets in source code.
 */
object CartBindConfig {
    val cartSecret: String
        get() = BuildConfig.CART_SECRET
}
