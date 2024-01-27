package ua.mezik.socketchat.model.message.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class DeleteChatRequest(
    @field:JsonProperty("chat_id") val chatId: Long
) : TransactionBase()