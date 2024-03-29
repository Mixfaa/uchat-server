package ua.mezik.uchat.model

import ua.mezik.uchat.model.message.requests.SerializedTransaction
import ua.mezik.uchat.model.message.requests.TransactionBase

interface ChatClient {
    fun sendToClient(transaction:TransactionBase)
    fun sendToClient(serializedTransaction: SerializedTransaction)
}