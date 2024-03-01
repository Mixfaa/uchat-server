package com.mezik.uchat.shared

import com.mezik.uchat.model.message.StatusResponse
import com.mezik.uchat.model.message.TransactionType

object CachedTransactions {
    private val cachedTransactions = HashMap<StatusResponse, Int>()
    private const val MAX_CACHED_TRANSACTIONS = 75

    val passwordNotMath = getStatusResponse(
        "Password not match",
        TransactionType.REQUEST_LOGIN,
    )
    val invalidRequest = getStatusResponse(
        "Can`t parse your request",
        TransactionType.STATUS_RESPONSE,
    )

    fun userNotAuthenticated(responseFor: TransactionType): StatusResponse =
        getStatusResponse("User not authenticated", responseFor)

    fun getStatusResponse(msg: String, respFor: TransactionType): StatusResponse {
        var statusResponse = cachedTransactions
            .keys.find { it.matchesByProps(msg, respFor) }

        if (statusResponse == null) {
            statusResponse = StatusResponse(msg, respFor)
            cachedTransactions[statusResponse] = 1

            if (cachedTransactions.size >= MAX_CACHED_TRANSACTIONS) {
                val iterator = cachedTransactions.iterator()

                while (iterator.hasNext()) {
                    val (cachedStatusResponse, usages) = iterator.next()
                    if (cachedStatusResponse == statusResponse || usages > 5)
                        continue

                    iterator.remove()
                }
            }
        } else
            cachedTransactions.compute(statusResponse) { response, usages ->
                return@compute if (usages == null) 1 else usages + 1
            }

        return statusResponse
    }
}