package ua.mezik.socketchat.model.message.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateChatRequest(
    @field:JsonProperty("name") val chatName: String,
    @field:JsonProperty("participants_ids") val participantsIds: List<Long>?
) : TransactionBase()
