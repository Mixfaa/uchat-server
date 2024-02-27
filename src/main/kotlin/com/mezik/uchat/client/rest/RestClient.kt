package com.mezik.uchat.client.rest

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.model.message.TransactionBase
import com.mezik.uchat.service.TransactionResolver
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

open class RestClient(val emitter: SseEmitter, private val resolver: TransactionResolver) : ChatClient {
    override fun sendToClient(transaction: TransactionBase) {
        emitter.send(transaction)
    }

    override fun handleTransaction(transaction: TransactionBase) {
        resolver.handleRequest(transaction, this)
    }

    override fun handleDisconnect() {
        resolver.clientDisconnected(this)
        emitter.complete()
    }
}