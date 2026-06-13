package com.yourbusiness.smartkart.data.api

import com.yourbusiness.smartkart.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CloudFunctionsApiClient {

    val service: CloudFunctionsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.CLOUD_FUNCTIONS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudFunctionsApiService::class.java)
    }
}
