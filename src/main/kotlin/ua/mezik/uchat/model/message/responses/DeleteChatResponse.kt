package ua.mezik.uchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.uchat.model.message.requests.TransactionBase

data class DeleteChatResponse(
    @field:JsonProperty("chat_id") val chatId: Long
) : TransactionBase()