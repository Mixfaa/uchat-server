package ua.mezik.socketchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.model.message.requests.TransactionBase
import ua.mezik.socketchat.model.Account
import ua.mezik.socketchat.model.Chat

data class ChatResponse(
    @field:JsonProperty("name") val name: String,
    @field:JsonProperty("chat_id") val chatId: Long,
    @field:JsonProperty("owner_id") val ownerId: Long,
    @field:JsonProperty("participants_ids") val participants: List<Long>,
    @field:JsonProperty("first_message_id") val firstMessageId: Long
) : TransactionBase() {
    constructor(chat: Chat) : this(
        chat.name,
        chat.id,
        chat.owner.id,
        chat.participants.map(Account::getId),
        chat.firstMessageId
    )
}
