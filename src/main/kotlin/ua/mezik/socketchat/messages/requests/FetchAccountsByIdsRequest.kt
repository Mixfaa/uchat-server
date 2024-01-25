package ua.mezik.socketchat.messages.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class FetchAccountsByIdsRequest(
    @field:JsonProperty("accounts_ids") val ids: List<Long>
) : TransactionBase()