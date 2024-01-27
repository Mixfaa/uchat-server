package ua.mezik.uchat.model.message.requests

import com.fasterxml.jackson.annotation.JsonProperty

class FetchAccountsRequest(
    @field:JsonProperty("query") val query: String?,
    @field:JsonProperty("page") val page: Int,
    @field:JsonProperty("limit") val limit: Int
) : TransactionBase()