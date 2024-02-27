package com.mezik.uchat.client.socket

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.model.message.TransactionBase
import com.mezik.uchat.model.message.TransactionUtils
import com.mezik.uchat.service.TransactionResolver
import com.mezik.uchat.shared.CachedTransactions
import kotlinx.coroutines.*
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

open class SocketClient(
    val socket: Socket,
    private val resolver: TransactionResolver
) : ChatClient {
    private val isConnected = AtomicBoolean(true)
    private val coroutineContext: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private val outputStream = socket.getOutputStream()
    private val inputReader = socket.getInputStream().bufferedReader()

    init {
        coroutineContext.launch {
            while (isConnected.get()) {
                val message = inputReader.readLine() ?: continue

                for (json in message.split("\n").filter { it.isNotBlank() || it.isNotEmpty() }) {
                    val request = TransactionUtils.deserializeTransaction(json).getOrNull()

                    if (request != null)
                        handleTransaction(request)
                    else
                        sendToClient(CachedTransactions.invalidRequest)
                }
            }
        }.invokeOnCompletion(::handleCoroutineCompletion)
    }

    private fun handleCoroutineCompletion(throwable: Throwable?) = when (throwable) {
        null -> {}
        else -> {
            throwable.printStackTrace()
            handleDisconnect()
        }
    }

    override fun sendToClient(transaction: TransactionBase) {
        coroutineContext.launch {
            outputStream.write(transaction.serialized)
        }.invokeOnCompletion(::handleCoroutineCompletion)
    }

    override fun handleTransaction(transaction: TransactionBase) {
        resolver.handleRequest(transaction, this)
    }

    override fun handleDisconnect() {
        if (!isConnected.get()) return

        coroutineContext.cancel()
        resolver.clientDisconnected(this)
        isConnected.set(false)
        socket.close()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SocketClient

        return socket == other.socket
    }

    override fun hashCode(): Int {
        return socket.hashCode()
    }
}
