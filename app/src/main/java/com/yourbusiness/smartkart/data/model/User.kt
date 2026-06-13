package com.yourbusiness.smartkart.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String,
    val phone: String,
    val name: String,
    val activeCart: String? = null,
    val createdAt: Timestamp? = null
)
