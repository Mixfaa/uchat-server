package ua.mezik.socketchat.handling

import arrow.core.Either
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ua.mezik.socketchat.misc.Transactions

/**
 * This service is here not to create lots of virtual threads just for sending heartbeat messages
 */
@Service
class HeartbeatSender {
    private val heartbeatReceivers: MutableMap<ChatClient, (Exception) -> Unit> = mutableMapOf()

    @Scheduled(fixedRate = 15000)
    private fun broadcastHeartbeat() {
        for ((client, fallback) in heartbeatReceivers) {
            try {
                client.sendToClient(Transactions.serializedHeartbeat)
            } catch (ex: Exception) {
                fallback(ex)
            }
        }
    }

    fun addReceiver(client: ChatClient, fallback: (Exception) -> Unit) {
        heartbeatReceivers[client] = fallback
    }

    fun removeReceiver(client: ChatClient) {
        heartbeatReceivers.remove(client)
    }
}