package ua.mezik.socketchat

import arrow.core.Either
import ua.mezik.socketchat.messages.requests.SerializedTransaction

typealias TransactionEither<T> = Either<SerializedTransaction, T>
