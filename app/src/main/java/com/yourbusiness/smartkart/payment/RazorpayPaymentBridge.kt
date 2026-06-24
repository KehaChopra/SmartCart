package com.yourbusiness.smartkart.payment

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RazorpayPaymentBridge {
    var inFlightOrderId: String = ""

    private val _events = MutableSharedFlow<SdkEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SdkEvent> = _events.asSharedFlow()

    sealed class SdkEvent {
        data class Success(
            val orderId: String,
            val paymentId: String,
            val signature: String
        ) : SdkEvent()

        data class Failure(
            val code: Int,
            val description: String
        ) : SdkEvent()
    }

    fun emit(event: SdkEvent) {
        _events.tryEmit(event)
    }
}
