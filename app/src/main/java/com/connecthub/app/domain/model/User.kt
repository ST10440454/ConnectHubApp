package com.connecthub.app.domain.model

data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val createdAtMillis: Long
)
