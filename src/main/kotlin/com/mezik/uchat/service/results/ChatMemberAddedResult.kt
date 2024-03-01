package com.mezik.uchat.service.results

import com.mezik.uchat.model.database.Account
import com.mezik.uchat.model.database.Chat

data class ChatMemberAddedResult(
    val member: Account,
    val chat: Chat
)