package com.yourbusiness.smartkart.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CloudFunctionsApiService {

    @POST("deleteItemFromCart")
    suspend fun deleteItemFromCart(
        @Body request: RemoveItemRequest
    ): Response<BindCartResponse>
}
