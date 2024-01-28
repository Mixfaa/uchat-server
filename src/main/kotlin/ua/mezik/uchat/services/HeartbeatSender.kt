package ua.mezik.uchat.services

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ua.mezik.uchat.misc.Transactions
import ua.mezik.uchat.model.ChatClient

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