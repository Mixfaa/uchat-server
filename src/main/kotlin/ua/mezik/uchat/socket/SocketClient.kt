package ua.mezik.uchat.socket

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ua.mezik.uchat.misc.Utils
import ua.mezik.uchat.model.message.requests.SerializedTransaction
import ua.mezik.uchat.model.message.requests.TransactionBase
import ua.mezik.uchat.services.ChatClient
import ua.mezik.uchat.services.HeartbeatSender
import ua.mezik.uchat.services.TransactionsResolver
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

private val coroutineContext: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

class SocketClient(
    private val clientSocket: Socket,
    private val heartbeatSender: HeartbeatSender,
    private val transactionsResolver: TransactionsResolver,
) : ChatClient {
    private var isConnected = AtomicBoolean(true)

    override fun sendToClient(transaction: TransactionBase) {
        if (clientSocket.isClosed) return
        clientSocket.getOutputStream().write(transaction.serialized)
    }
    override fun sendToClient(serializedTransaction: SerializedTransaction) {
        if (clientSocket.isClosed) return
        clientSocket.getOutputStream().write(serializedTransaction)
    }

    private fun heartbeatFallback(ex: Exception) {
        isConnected.set(false)
        transactionsResolver.clientDisconnected(this)
        clientSocket.close()

        heartbeatSender.removeReceiver(this)
    }

    fun handleAsync() {
        heartbeatSender.addReceiver(this, ::heartbeatFallback)

        coroutineContext.launch {
            val inputStream = clientSocket.getInputStream()
            while (isConnected.get()) {
                try {
                    var availableToRead = inputStream.available()
                    while (availableToRead <= 0) {
                        availableToRead = inputStream.available()
                        Thread.sleep(50)
                    }

                    if (!isConnected.get()) return@launch

                    val rawMessage = String(inputStream.readNBytes(availableToRead))
                    println("message: $rawMessage message end;")
                    for (json in Utils.splitJsons(rawMessage)) {
                        val probablyRequest = Utils.jsonMapper.readValue<TransactionBase>(json)
                        println(probablyRequest)

                        val response = transactionsResolver.handleRequest(probablyRequest, this@SocketClient)
                            ?: continue

                        sendToClient(response)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return@launch
                }
            }
        }
    }
}