package com.yourbusiness.smartkart.data.api

import com.yourbusiness.smartkart.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CartApiClient {

    val service: CartApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BIND_CART_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CartApiService::class.java)
    }
}
