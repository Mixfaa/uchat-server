package ua.mezik.socketchat.handling

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.net.ServerSocket

@Service
class SocketServer(
    @Value("socket.ip4") private val inetAddress: String,
    @Value("socket.backlog") private val backlog:Int,
    @Value("socket.port") private val port:Int,
    private val transactionsResolver: TransactionsResolver
) {
    private var socketAcceptingThread: Thread = Thread(this::acceptSockets)

    init {
        socketAcceptingThread.start()
    }

    private fun acceptSockets() {
        println("Socket is listening...")
        val socket = ServerSocket(port, backlog, InetAddress.getByName(inetAddress))
        while (socketAcceptingThread.isInterrupted) {
            val client = socket.accept()
            val handler = ClientHandler(client, transactionsResolver)
            handler.handleAsync()
        }
    }
}