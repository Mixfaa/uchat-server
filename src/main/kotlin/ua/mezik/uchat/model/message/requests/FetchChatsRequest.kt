package ua.mezik.uchat.model.message.requests

import com.fasterxml.jackson.annotation.JsonProperty

class FetchChatsRequest(
    @field:JsonProperty("page") val page: Int,
    @field:JsonProperty("limit") val limit: Int
) : TransactionBase()