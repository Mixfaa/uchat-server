package ua.mezik.socketchat.model.message.responses

import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.model.Account
import ua.mezik.socketchat.model.message.requests.TransactionBase

data class FetchAccountsResponse(
    @field: JsonProperty("accounts") val accounts: List<Account>,
    @field:JsonProperty("query") val query: String?
) : TransactionBase()