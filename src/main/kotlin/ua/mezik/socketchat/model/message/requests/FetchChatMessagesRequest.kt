package ua.mezik.socketchat.model.message.requests

import com.fasterxml.jackson.annotation.JsonProperty

class FetchChatMessagesRequest(
    @field:JsonProperty("chat_id") val chatId: Long,
    @field:JsonProperty("page") val page: Int,
    @field:JsonProperty("limit") val limit: Int
) : TransactionBase()