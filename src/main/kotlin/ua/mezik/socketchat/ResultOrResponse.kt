package ua.mezik.socketchat

import ua.mezik.socketchat.messages.requests.SerializedTransaction
import ua.mezik.socketchat.messages.requests.TransactionType

abstract class ResultOrResponse<T> {

    abstract val value: T?
    abstract val response: SerializedTransaction?

    companion object {
        fun <T> success(value: T): ResultOrResponse<T> {
            return SuccessResult(value)
        }

        fun <T> failure(message: String, responseFor: TransactionType, fail: Boolean = true): ResultOrResponse<T> {
            return FailureResult(Transactions.serializeStatusResponse(message, responseFor, fail))
        }

        fun <T> accessDenied(responseFor: TransactionType): ResultOrResponse<T> {
            return FailureResult(Transactions.accessDenied(responseFor))
        }

        fun <T> chatNotFound(responseFor: TransactionType): ResultOrResponse<T> {
            return failure("Chat not found", responseFor)
        }
    }

    operator fun component1(): T? = value
    operator fun component2(): SerializedTransaction? = response

    class SuccessResult<T>(override val value: T) : ResultOrResponse<T>() {
        override val response: SerializedTransaction? = null
    }

    class FailureResult<T>(override val response: SerializedTransaction) : ResultOrResponse<T>() {
        override val value: T? = null
    }
}
