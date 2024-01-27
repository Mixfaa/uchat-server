package ua.mezik.socketchat.logic.services

import arrow.core.right
import org.springframework.stereotype.Service
import ua.mezik.socketchat.logic.ClientHandler
import ua.mezik.socketchat.messages.requests.TransactionBase
import ua.mezik.socketchat.models.Account
import java.util.concurrent.CopyOnWriteArrayList

@Service
class ConnectionsManager {
    private val persistedUsers: MutableList<PersistedConnection> = CopyOnWriteArrayList()

    fun persistConnectionFrom(account: Account, client: ClientHandler) {
        val persistedConnection = persistedConnectionByAccount(account)

        if (persistedConnection == null)
            addPersistedConnection(account, client)
        else
            persistedConnection.clients.add(client)
    }

    fun persistedConnectionByAccount(account: Account): PersistedConnection? {
        return persistedUsers.firstOrNull { it.account == account }
    }

    private fun addPersistedConnection(account: Account, clientHandler: ClientHandler) {
        persistedUsers.add(PersistedConnection(account, mutableListOf(clientHandler)))
    }

    fun accountFromClient(client: ClientHandler): Account? {
        return persistedUsers.firstOrNull { it.clients.contains(client) }?.account
    }

    fun clientDisconnected(client: ClientHandler) {
        val persistedConnection = persistedUsers.firstOrNull { it.clients.contains(client) } ?: return
        persistedConnection.clients.remove(client)
    }

    fun sendTransactionToClients(accounts: List<Account>, transaction: TransactionBase) {
        for (account in accounts)
            sendTransactionToClient(account, transaction)
    }

    fun sendTransactionToClientsExcept(accounts: List<Account>, exception: Account, transaction: TransactionBase) {
        for (account in accounts) {
            if (account == exception) continue
            sendTransactionToClient(account, transaction)
        }
    }

    fun sendTransactionToClient(account: Account, transaction: TransactionBase): Boolean {
        val clients = persistedUsers.firstOrNull { it.account == account }?.clients
            ?: return false

        clients.forEach { it.sendToSocket(transaction) }
        return true
    }
}