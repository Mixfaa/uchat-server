package ua.mezik.socketchat.misc

import arrow.core.Either
import ua.mezik.socketchat.model.message.requests.SerializedTransaction
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
