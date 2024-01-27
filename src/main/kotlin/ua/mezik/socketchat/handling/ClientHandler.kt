package ua.mezik.socketchat.handling

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ua.mezik.socketchat.misc.Transactions
import ua.mezik.socketchat.misc.Utils
import ua.mezik.socketchat.model.message.requests.SerializedTransaction
import ua.mezik.socketchat.model.message.requests.TransactionBase
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ClientHandler(
    private val clientSocket: Socket,
    private val transactionsResolver: TransactionsResolver,
    private val coroutineContext: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private var isConnected = AtomicBoolean(true)

    fun sendToSocket(transaction: TransactionBase) {
        if (clientSocket.isClosed) return
        clientSocket.getOutputStream().write(transaction.serialized)
    }

    fun sendToSocket(serialized: SerializedTransaction) {
        if (clientSocket.isClosed) return
        clientSocket.getOutputStream().write(serialized)
    }

    private fun sendHeartbeat() {
        try {
            clientSocket.getOutputStream().write(Transactions.serializedHeartbeat)
        } catch (ex: Exception) {
            ex.printStackTrace()
            isConnected.set(false)
            transactionsResolver.clientDisconnected(this)
            clientSocket.close()
        }
    }

    fun handleAsync() {
        coroutineContext.launch {
            while (isConnected.get()) {
                Thread.sleep(15000)
                sendHeartbeat()
            }
        }
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

                        val response = transactionsResolver.handleRequest(probablyRequest, this@ClientHandler)
                            ?: continue

                        sendToSocket(response)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return@launch
                }
            }
        }
    }
}