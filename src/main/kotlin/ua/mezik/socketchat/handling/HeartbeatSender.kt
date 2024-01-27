package ua.mezik.socketchat.handling

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ua.mezik.socketchat.misc.Transactions

/**
 * This service is here not to create lots of virtual threads just for sending heartbeat messages
 */
@Service
class HeartbeatSender {
    private val heartbeatReceivers: MutableMap<ClientHandler, (Exception) -> Unit> = mutableMapOf()

    @Scheduled(fixedRate = 15000)
    private fun broadcastHeartbeat() {
        for ((client, fallback) in heartbeatReceivers) {
            try {
                client.sendToSocket(Transactions.serializedHeartbeat)
            } catch (ex: Exception) {
                fallback(ex)
            }
        }
    }

    fun addReceiver(clientHandler: ClientHandler, fallback: (Exception) -> Unit) {
        heartbeatReceivers[clientHandler] = fallback
    }

    fun removeReceiver(clientHandler: ClientHandler) {
        heartbeatReceivers.remove(clientHandler)
    }
}