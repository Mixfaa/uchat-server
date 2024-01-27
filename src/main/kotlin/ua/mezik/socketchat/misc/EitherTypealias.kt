package ua.mezik.socketchat.misc

import arrow.core.Either
import ua.mezik.socketchat.model.message.requests.SerializedTransaction

typealias TransactionEither<T> = Either<SerializedTransaction, T>
