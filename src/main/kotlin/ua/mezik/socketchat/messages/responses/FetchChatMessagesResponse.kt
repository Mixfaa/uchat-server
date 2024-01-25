package ua.mezik.socketchat.messages.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.messages.requests.TransactionBase

class FetchChatMessagesResponse(
    @field:JsonProperty("chat_id") val chatId: Long,
    @field:JsonProperty("messages") val messages: List<MessageResponse>
) : TransactionBase()