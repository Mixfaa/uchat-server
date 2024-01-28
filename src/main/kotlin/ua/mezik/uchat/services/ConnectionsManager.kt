package ua.mezik.uchat.services

import org.springframework.stereotype.Service
import ua.mezik.uchat.model.PersistedConnection
import ua.mezik.uchat.model.message.requests.TransactionBase
import ua.mezik.uchat.model.Account
import java.util.concurrent.CopyOnWriteArrayList

@Service
class ConnectionsManager {
    private val persistedUsers: MutableList<PersistedConnection> = CopyOnWriteArrayList()

    fun persistConnectionFrom(account: Account, client: ChatClient) {
        val persistedConnection = persistedUsers.firstOrNull { it.account == account }

        if (persistedConnection == null)
            addPersistedConnection(account, client)
        else
            persistedConnection.clients.add(client)
    }

    private fun addPersistedConnection(account: Account, chat: ChatClient) {
        persistedUsers.add(PersistedConnection(account, mutableListOf(chat)))
    }

    fun findClientAccount(client: ChatClient): Account? {
        return persistedUsers.firstOrNull { it.clients.contains(client) }?.account
    }

    fun clientDisconnected(client: ChatClient) {
        val persistedConnection = persistedUsers.firstOrNull { it.clients.contains(client) } ?: return
        persistedConnection.clients.remove(client)
    }

    fun sendTransactionToClients(accounts: Iterable<Account>, transaction: TransactionBase) {
        for (account in accounts)
            sendTransactionToClient(account, transaction)
    }

    fun sendTransactionToClientsExcept(accounts: Iterable<Account>, exception: Account, transaction: TransactionBase) {
        for (account in accounts) {
            if (account == exception) continue
            sendTransactionToClient(account, transaction)
        }
    }

    fun sendTransactionToClient(account: Account, transaction: TransactionBase) {
        val clients = persistedUsers.firstOrNull { it.account == account }?.clients
            ?: return

        clients.forEach { it.sendToClient(transaction) }
    }
}