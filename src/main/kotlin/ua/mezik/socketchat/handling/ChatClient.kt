package ua.mezik.socketchat.handling

import ua.mezik.socketchat.model.message.requests.SerializedTransaction
import ua.mezik.socketchat.model.message.requests.TransactionBase

interface ChatClient {
    fun sendToClient(transaction:TransactionBase)
    fun sendToClient(serializedTransaction: SerializedTransaction)
}