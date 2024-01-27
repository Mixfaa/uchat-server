package ua.mezik.uchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.uchat.model.message.requests.TransactionBase

class FetchChatsResponse(
    @field:JsonProperty("chats") val chats: List<ChatResponse>
) : TransactionBase()