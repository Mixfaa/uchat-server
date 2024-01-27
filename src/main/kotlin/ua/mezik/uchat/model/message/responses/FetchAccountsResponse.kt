package ua.mezik.uchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.uchat.model.Account
import ua.mezik.uchat.model.message.requests.TransactionBase

data class FetchAccountsResponse(
    @field: JsonProperty("accounts") val accounts: List<Account>,
    @field:JsonProperty("query") val query: String?
) : TransactionBase()