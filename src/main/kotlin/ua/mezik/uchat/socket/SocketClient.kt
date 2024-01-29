package ua.mezik.uchat.socket

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import ua.mezik.uchat.misc.Utils
import ua.mezik.uchat.model.message.requests.SerializedTransaction
import ua.mezik.uchat.model.message.requests.TransactionBase
import ua.mezik.uchat.model.ChatClient
import ua.mezik.uchat.services.HeartbeatSender
import ua.mezik.uchat.services.TransactionsResolver
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

private val coroutineContext: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

class SocketClient(
    private val clientSocket: Socket,
    private val heartbeatSender: HeartbeatSender,
    private val transactionsResolver: TransactionsResolver,
) : ChatClient, AutoCloseable {
    private var isConnected = AtomicBoolean(true)

    override fun sendToClient(transaction: TransactionBase) {
        if (clientSocket.isClosed) return
        clientSocket.getOutputStream().write(transaction.serialized)
    }

    override fun sendToClient(serializedTransaction: SerializedTransaction) {
        if (clientSocket.isClosed) return
        clientSocket.getOutputStream().write(serializedTransaction)
    }


    fun startHandling() {
        heartbeatSender.addReceiver(this) { _ -> close() }

        coroutineContext.launch {
            val inputStream = clientSocket.getInputStream()
            val messageBuilder = StringBuilder()
            while (isConnected.get()) {
                try {
                    var lastDelimiterIndex: Int
                    do {
                        var availableToRead = 0
                        while (availableToRead <= 0 && isConnected.get()) {
                            availableToRead = inputStream.available()
                            delay(50)
                        }

                        messageBuilder.append(String(inputStream.readNBytes(availableToRead)))
                        lastDelimiterIndex = messageBuilder.lastIndexOf('\n')
                    } while (lastDelimiterIndex == -1)

                    val message = messageBuilder.substring(0, lastDelimiterIndex + 1)
                    val cleared = messageBuilder.removeRange(0, lastDelimiterIndex + 1)
                    messageBuilder.clear()
                    messageBuilder.append(cleared)

                    for (json in Utils.splitJsons(message)) {
                        val probablyRequest = Utils.jsonMapper.readValue<TransactionBase>(json)
                        println(probablyRequest)

                        val response = transactionsResolver.handleRequest(probablyRequest, this@SocketClient)
                            ?: continue

                        sendToClient(response)
                    }

                } catch (ex: Exception) {
                    ex.printStackTrace()
                    close()
                    return@launch
                }
            }
        }
    }

    override fun close() {
        isConnected.set(false)
        transactionsResolver.clientDisconnected(this)
        clientSocket.close()

        heartbeatSender.removeReceiver(this)
    }
}
