package ua.mezik.socketchat.messages.responses

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.messages.requests.TransactionBase
import ua.mezik.socketchat.models.ChatMessage
import ua.mezik.socketchat.models.MessageType
import ua.mezik.socketchat.models.messages.FileMessage
import ua.mezik.socketchat.models.messages.TextMessage

data class MessageResponse(
    @field:JsonProperty("message_id") val messageId: Long,
    @field:JsonProperty("owner_id") val ownerId: Long,
    @field:JsonProperty("chat_id") val chatId: Long,
    @field:JsonProperty("timestamp") val timestamp: Long,
    @field:JsonProperty("message_type") val messageType: MessageType,
    @field:JsonProperty("buffer") val message: String,
    @field:JsonProperty("is_edited") val edited: Boolean = false
) : TransactionBase() {
    companion object {
        @JsonIgnore
        fun fromMessage(message: ChatMessage): MessageResponse {
            val (buffer, edited) = when (message) {
                is TextMessage -> Pair(message.text, message.isEdited)
                is FileMessage -> Pair(message.link, false)
                else -> Pair("empy msg", false)
            }

            return MessageResponse(
                message.id,
                message.owner.id,
                message.chat.id,
                message.timestamp.time,
                message.type,
                buffer,
                edited
            )
        }

    }
}
