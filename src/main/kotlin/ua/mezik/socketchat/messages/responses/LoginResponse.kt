package ua.mezik.socketchat.messages.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.messages.requests.TransactionBase
import ua.mezik.socketchat.models.Account

data class LoginResponse(
    @field:JsonProperty("user") val user: Account,
    @field:JsonProperty("chats_ids") val chatsIds: List<Long>
) : TransactionBase()