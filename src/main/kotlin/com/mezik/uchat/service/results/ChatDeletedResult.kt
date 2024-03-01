package com.mezik.uchat.service.results

import com.mezik.uchat.model.database.Account

data class ChatDeletedResult(
    val chatId: Long,
    val members: List<Account>
)