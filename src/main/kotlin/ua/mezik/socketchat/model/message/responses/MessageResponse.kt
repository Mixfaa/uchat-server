package ua.mezik.socketchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.model.ChatMessage
import ua.mezik.socketchat.model.message.requests.TransactionBase
import ua.mezik.socketchat.model.MessageType
import ua.mezik.socketchat.model.message.FileMessage
import ua.mezik.socketchat.model.message.TextMessage

data class MessageResponse(
    @field:JsonProperty("message_id") val messageId: Long,
    @field:JsonProperty("owner_id") val ownerId: Long,
    @field:JsonProperty("chat_id") val chatId: Long,
    @field:JsonProperty("timestamp") val timestamp: Long,
    @field:JsonProperty("message_type") val messageType: MessageType,
    @field:JsonProperty("buffer") val message: String,
    @field:JsonProperty("is_edited") val edited: Boolean = false
) : TransactionBase() {

    constructor(message: ChatMessage) : this(
        message.id,
        message.owner.id,
        message.chat.id,
        message.timestamp.time,
        message.type,
        when (message) {
            is TextMessage -> message.text
            is FileMessage -> message.link
            else -> "empty msg"
        },
        when (message) {
            is TextMessage -> message.isEdited
            else -> false
        }
    )
}
