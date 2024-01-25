package ua.mezik.socketchat.messages.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.messages.requests.TransactionBase
import ua.mezik.socketchat.models.messages.TextMessage

data class MessageEditResponse(
    @field:JsonProperty("message_id") val messageId: Long,
    @field:JsonProperty("chat_id") val chatId: Long,
    @field:JsonProperty("new_buffer") val newBuffer: String
) : TransactionBase() {
    companion object {
        fun fromMessage(message: TextMessage): MessageEditResponse {
            return MessageEditResponse(message.id, message.chat.id, message.text)
        }
    }
}