package com.mezik.uchat.model.database

import com.mezik.uchat.model.message.EncryptedMessage
import com.mezik.uchat.service.SequencedIdGenerator
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

@Document("file_message")
class FileMessage(
    owner: Account,
    chat: Chat,
    type: MessageType,
    timestamp: Long,
    keyId: Long,
    val buffer: EncryptedMessage,
    databaseId: ObjectId = ObjectId(), // database requirement
    id: Long = SequencedIdGenerator.instance.generateId("chat_message")
) : ChatMessage(owner, chat, type, timestamp, keyId, databaseId = databaseId, id = id)
