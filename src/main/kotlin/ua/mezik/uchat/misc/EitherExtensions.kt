package ua.mezik.uchat.misc

import arrow.core.Either
import ua.mezik.uchat.model.message.requests.SerializedTransaction
import ua.mezik.uchat.model.message.requests.TransactionBase
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

typealias TransactionEither<T> = Either<SerializedTransaction, T>

@OptIn(ExperimentalContracts::class)
fun <R,C> Either<C,R>.foldRight(ifRight: (right: R) -> C): C {
    contract {
        callsInPlace(ifRight, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Either.Right -> ifRight(value)
        is Either.Left -> value
    }
}

fun <T> Either<Throwable,T>.mapLeftToStatusResponseFor(request: TransactionBase) : TransactionEither<T> {
    return this.mapLeft { Transactions.serializeStatusResponse(it.localizedMessage, request.type, true) }
}