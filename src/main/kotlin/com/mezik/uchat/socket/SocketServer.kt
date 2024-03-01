package com.mezik.uchat.socket

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.client.factory.ChatClientFactoryBuilder
import com.mezik.uchat.client.factory.features.WithDisconnectCallback
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList
import javax.net.ssl.SSLException
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import javax.net.ssl.SSLSocket

@Service
final class SocketServer(
    @Value("\${socket.ip4}") private val inetAddress: String,
    @Value("\${socket.backlog}") private val backlog: Int,
    @Value("\${socket.port}") private val port: Int,
    sslServerSocketFactory: SSLServerSocketFactory,
    factoryBuilder: ChatClientFactoryBuilder
) {
    private val socketClients = CopyOnWriteArrayList<ChatClient>()

    private val chatClientFactory = factoryBuilder
        .createFactory(WithDisconnectCallback(socketClients::remove))

    init {
        logger.info("Server socket started...")
        val serverSocket =
            sslServerSocketFactory.createServerSocket(
                port,
                backlog,
                InetAddress.getByName(inetAddress)
            ) as SSLServerSocket

        coroutineContext.launch {
            while (this.isActive) {
                val client = serverSocket.accept() as SSLSocket

                client.addHandshakeCompletedListener {
                    socketClients.add(chatClientFactory.newSocketClient(it.socket))
                }
                try {
                    client.startHandshake()
                } catch (ex: SSLException) {
                    logger.error(ex.localizedMessage)
                }
            }
        }
    }

    companion object {
        private val coroutineContext = CoroutineScope(Dispatchers.Default + SupervisorJob())
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}