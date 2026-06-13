package com.yourbusiness.smartkart.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CartApiService {

    @POST(".")
    suspend fun bindCartToUser(
        @Body request: BindCartRequest
    ): Response<BindCartResponse>
}
