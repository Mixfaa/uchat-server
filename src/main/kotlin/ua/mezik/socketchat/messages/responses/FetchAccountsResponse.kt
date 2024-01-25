package ua.mezik.socketchat.messages.responses

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import ua.mezik.socketchat.messages.requests.TransactionType
import ua.mezik.socketchat.messages.requests.TransactionBase
import ua.mezik.socketchat.models.Account

data class FetchAccountsResponse(
    @field: JsonProperty("accounts") val accounts: List<Account>,
    @field:JsonProperty("query") val query: String?
) : TransactionBase()