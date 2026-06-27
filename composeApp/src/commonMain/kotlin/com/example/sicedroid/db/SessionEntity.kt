package com.example.sicedroid.db

data class SessionEntity(
    val matricula: String,
    val password: String,
    val is_logged_in: Long = 0,
    val last_login: Long = 0
)
