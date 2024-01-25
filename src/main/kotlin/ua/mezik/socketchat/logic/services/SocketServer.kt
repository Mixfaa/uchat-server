package ua.mezik.socketchat.logic.services

import org.springframework.stereotype.Service
import ua.mezik.socketchat.logic.ClientHandler
import ua.mezik.socketchat.logic.TransactionsResolver
import java.net.InetAddress
import java.net.ServerSocket

@Service
class SocketServer(
    private val transactionsResolver: TransactionsResolver
) {
    private var socketAcceptingThread: Thread = Thread(this::acceptSockets)

    init {
        socketAcceptingThread.start()
    }

    private fun acceptSockets() {
        println("Socket is listening...")
        val socket = ServerSocket(8080, 5, InetAddress.getByName("192.168.0.213"))
        while (true) {
            val client = socket.accept()
            val handler = ClientHandler(client, transactionsResolver)
            handler.handleAsync()
        }
    }
}