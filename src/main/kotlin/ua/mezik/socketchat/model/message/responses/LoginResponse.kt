package ua.mezik.socketchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.model.message.requests.TransactionBase
import ua.mezik.socketchat.model.Account

data class LoginResponse(
    @field:JsonProperty("user") val user: Account,
    @field:JsonProperty("chats_ids") val chatsIds: List<Long>
) : TransactionBase()