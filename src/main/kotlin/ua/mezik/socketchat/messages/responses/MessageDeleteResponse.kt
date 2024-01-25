package ua.mezik.socketchat.messages.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.messages.requests.TransactionBase

data class MessageDeleteResponse(
    @field:JsonProperty("message_id") val messageId: Long,
    @field:JsonProperty("chat_id") val chatId: Long
) : TransactionBase()