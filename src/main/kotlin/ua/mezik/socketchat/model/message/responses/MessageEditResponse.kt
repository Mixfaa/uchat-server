package ua.mezik.socketchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.model.message.requests.TransactionBase
import ua.mezik.socketchat.model.message.TextMessage

data class MessageEditResponse(
    @field:JsonProperty("message_id") val messageId: Long,
    @field:JsonProperty("chat_id") val chatId: Long,
    @field:JsonProperty("new_buffer") val newBuffer: String
) : TransactionBase() {
    constructor(message: TextMessage) : this(message.id, message.chat.id, message.text)
}