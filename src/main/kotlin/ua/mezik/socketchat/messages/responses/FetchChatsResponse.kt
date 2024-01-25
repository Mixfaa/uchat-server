package ua.mezik.socketchat.messages.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.messages.requests.TransactionBase

class FetchChatsResponse(
    @field:JsonProperty("chats") val chats: List<ChatResponse>
) : TransactionBase()