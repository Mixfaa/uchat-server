package ua.mezik.socketchat.messages.requests

import com.fasterxml.jackson.annotation.JsonProperty

class MessageEditRequest(
    @field:JsonProperty("message_id") val messageId: Long,
    @field:JsonProperty("buffer") val buffer: String
) : TransactionBase()
