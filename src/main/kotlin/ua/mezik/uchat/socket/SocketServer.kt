package ua.mezik.uchat.socket

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ua.mezik.uchat.services.HeartbeatSender
import ua.mezik.uchat.services.TransactionsResolver
import java.net.InetAddress
import java.net.ServerSocket

@Service
class SocketServer(
    @Value("socket.ip4") private val inetAddress: String,
    @Value("socket.backlog") private val backlog: Int,
    @Value("socket.port") private val port: Int,
    private val transactionsResolver: TransactionsResolver,
    private val heartbeatSender: HeartbeatSender
) {
    private var socketAcceptingThread: Thread = Thread(this::acceptSockets).also { it.start() }

    private fun acceptSockets() {
        println("Socket is listening...")
        val socket = ServerSocket(port, backlog, InetAddress.getByName(inetAddress))
        while (socketAcceptingThread.isInterrupted) {
            val client = socket.accept()

            SocketClient(client, heartbeatSender, transactionsResolver).startHandling()
        }
    }
}