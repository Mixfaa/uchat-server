package com.mezik.uchat.model.database

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mezik.uchat.model.message.MemberDecryptionKey
import com.mezik.uchat.service.SequencedIdGenerator
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("chat")
data class Chat(
    val name: String,
    val owner: Account,
    val members: List<Account> = listOf(),
    val membersDecryptionKeys: List<MemberDecryptionKey>,
    @field:JsonIgnore @Id var databaseId: ObjectId = ObjectId(),
    val id: Long = SequencedIdGenerator.instance.generateId("chat")
)