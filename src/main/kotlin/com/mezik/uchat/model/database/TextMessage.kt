package com.mezik.uchat.model.database

import com.mezik.uchat.model.message.EncryptedMessage
import com.mezik.uchat.service.SequencedIdGenerator
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

@Document("text_message")
class TextMessage(
    owner: Account,
    chat: Chat,
    type: MessageType,
    timestamp: Long,
    keyId: Long,
    val text: EncryptedMessage,
    val isEdited: Boolean = false,
    databaseId: ObjectId = ObjectId(), // database requirement
    id: Long = SequencedIdGenerator.instance.generateId("chat_message")
) : ChatMessage(owner, chat, type, timestamp, keyId, databaseId, id) {
    fun copy(
        owner: Account = this.owner,
        chat: Chat = this.chat,
        type: MessageType = this.type,
        timestamp: Long = this.timestamp,
        keyId: Long = this.keyId,
        text: EncryptedMessage = this.text,
        isEdited: Boolean = this.isEdited,
        databaseId: ObjectId = this.databaseId,
        id: Long = this.id
    ) = TextMessage(owner, chat, type, timestamp, keyId, text, isEdited, databaseId, id)
}
