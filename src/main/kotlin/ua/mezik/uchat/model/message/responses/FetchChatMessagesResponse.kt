package ua.mezik.uchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.uchat.model.message.requests.TransactionBase

class FetchChatMessagesResponse(
    @field:JsonProperty("chat_id") val chatId: Long,
    @field:JsonProperty("messages") val messages: List<MessageResponse>
) : TransactionBase()