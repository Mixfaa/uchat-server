package ua.mezik.uchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.uchat.model.message.requests.TransactionBase
import ua.mezik.uchat.model.Account

data class LoginResponse(
    @field:JsonProperty("user") val user: Account,
    @field:JsonProperty("chats_ids") val chatsIds: List<Long>
) : TransactionBase()