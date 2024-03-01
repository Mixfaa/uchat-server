package com.mezik.uchat.service.results

import com.mezik.uchat.model.database.Chat

data class ChatMessageDeletedResult(
    val chat: Chat,
    val messageId: Long
)