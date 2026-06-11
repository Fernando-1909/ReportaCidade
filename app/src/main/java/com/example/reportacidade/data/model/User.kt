package com.example.reportacidade.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val neighborhood: String = "",
    val profileImageUrl: String? = null
)
