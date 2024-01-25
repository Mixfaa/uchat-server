package ua.mezik.socketchat.messages.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.messages.requests.TransactionBase
import ua.mezik.socketchat.messages.requests.TransactionType

data class StatusResponse(
    @field:JsonProperty("message") var message: String,
    @field:JsonProperty("response_for") var responseFor: TransactionType,
    @field:JsonProperty("is_failed") var fail: Boolean = true,
) : TransactionBase()