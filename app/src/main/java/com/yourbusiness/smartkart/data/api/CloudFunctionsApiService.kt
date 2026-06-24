package com.yourbusiness.smartkart.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CloudFunctionsApiService {

    @POST("deleteItemFromCart")
    suspend fun deleteItemFromCart(
        @Body request: RemoveItemRequest
    ): Response<BindCartResponse>

    @POST("createOrder")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): Response<CreateOrderResponse>

    @POST("verifyPayment")
    suspend fun verifyPayment(
        @Body request: VerifyPaymentRequest
    ): Response<VerifyPaymentResponse>
}
