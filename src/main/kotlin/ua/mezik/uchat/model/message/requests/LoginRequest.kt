package ua.mezik.uchat.model.message.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class LoginRequest(
    @field:JsonProperty("username") val username: String,
    @field:JsonProperty("password") val password: String
) : TransactionBase()
