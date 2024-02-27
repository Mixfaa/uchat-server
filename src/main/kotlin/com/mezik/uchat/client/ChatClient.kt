package com.mezik.uchat.client

import com.mezik.uchat.model.message.TransactionBase

interface ChatClient {
    fun sendToClient(transaction: TransactionBase)
    fun handleTransaction(transaction: TransactionBase)
    fun handleDisconnect()
}