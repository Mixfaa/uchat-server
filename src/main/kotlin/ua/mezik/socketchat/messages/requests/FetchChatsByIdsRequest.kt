package ua.mezik.socketchat.messages.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class FetchChatsByIdsRequest(
    @field:JsonProperty("chats_ids") val chatsIds: List<Long>
) : TransactionBase()