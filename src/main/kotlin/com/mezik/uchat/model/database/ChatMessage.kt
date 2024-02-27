package com.mezik.uchat.model.database

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mezik.uchat.model.message.MessageRequest
import com.mezik.uchat.service.SequencedIdGenerator
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("chat_message")
sealed class ChatMessage(
    val owner: Account,
    @field:JsonIgnore val chat: Chat,
    val type: MessageType,
    val timestamp: Long,
    val keyId: Long,
    @field:JsonIgnore @Id var databaseId: ObjectId = ObjectId(),
    var id: Long = SequencedIdGenerator.instance.generateId("chat_message")
) {
    companion object {
        fun createBasedOnRequest(request: MessageRequest, account: Account, chat: Chat): ChatMessage {
            val timestamp = Calendar.getInstance().timeInMillis
            return when (request.messageType) {
                MessageType.TEXT -> TextMessage(
                    account,
                    chat,
                    request.messageType,
                    timestamp,
                    request.keyId,
                    request.buffer,
                )

                MessageType.FILE -> FileMessage(
                    account,
                    chat,
                    request.messageType,
                    timestamp,
                    request.keyId,
                    request.buffer,
                )
            }
        }
    }
}
