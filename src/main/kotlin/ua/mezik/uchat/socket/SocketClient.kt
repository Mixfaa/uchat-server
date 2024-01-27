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
import java.io.InputStream
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


    fun handleAsync() {
        heartbeatSender.addReceiver(this) { _ -> close() }

        coroutineContext.launch {
            val inputStream = clientSocket.getInputStream()
            while (isConnected.get()) {
                try {
                    var availableToRead = inputStream.available()
                    
                    while (availableToRead <= 0 && isConnected.get()) {
                        availableToRead = inputStream.available()
                        Thread.sleep(50)
                    }

                    val message = String(inputStream.readNBytes(availableToRead))

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



/*
    fun handleAsync() {
        heartbeatSender.addReceiver(this) { _ -> close() }

        coroutineContext.launch {
            val inputStream = clientSocket.getInputStream()
            val messageBuilder = StringBuilder()
            while (isConnected.get()) {
                try {
                    if (!isConnected.get()) return@launch

                    do {
                        messageBuilder.append(readFromSocket(inputStream))
                    } while (messageBuilder.lastOrNull() != '\n' || messageBuilder.contains('\n')) // we will wait and read from socket until we get complete message

                    val lastIndexOfDelimiter = messageBuilder.lastIndexOf('\n')

                    val completeMessage: String

                    if (messageBuilder.lastIndex == lastIndexOfDelimiter) {
                        completeMessage = messageBuilder.toString()
                        messageBuilder.clear()
                    } else {
                        completeMessage = messageBuilder.substring(0, lastIndexOfDelimiter + 1)
                        messageBuilder.deleteRange(0, lastIndexOfDelimiter + 1) // test it
                    }

                    for (json in Utils.splitJsons(completeMessage)) {
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
 */