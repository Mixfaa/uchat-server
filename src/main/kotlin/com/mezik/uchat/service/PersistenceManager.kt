package com.mezik.uchat.service

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.model.database.Account
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

private data class PersistedConnection(val account: Account, val clients: MutableSet<ChatClient>)

@Service
class PersistenceManager {
    private val persistedUsers: MutableList<PersistedConnection> =
        CopyOnWriteArrayList()

    fun persistConnectionFrom(account: Account, client: ChatClient) {
        logger.info("Persisting connections $account $client")

        val persistedConnection = persistedUsers.firstOrNull { it.account == account }

        if (persistedConnection == null)
            addPersistedConnection(account, client)
        else
            persistedConnection.clients.add(client)
    }

    private fun addPersistedConnection(account: Account, client: ChatClient) {
        persistedUsers.add(PersistedConnection(account, CopyOnWriteArraySet(listOf(client))))
    }

    fun findAccountsClients(accounts: Iterable<Account>): Iterable<ChatClient>? {
        val clientsSeq = persistedUsers.asSequence()
            .filter { accounts.contains(it.account) && it.clients.isNotEmpty()}
            .map { it.clients }

        val totalSize = clientsSeq.sumOf { it.size }
        if (totalSize == 0) return null

        val clients = ArrayList<ChatClient>(totalSize)
        clientsSeq.forEach(clients::addAll)

        return clients
    }

    fun findAccountsClientsAsMap(accounts: Iterable<Account>): Map<Account, Set<ChatClient>> {
        return persistedUsers.asSequence()
            .filter { it.account in accounts && it.clients.isNotEmpty()}
            .map { it.account to it.clients  }
            .toMap()
    }

    fun findClientAccount(client: ChatClient): Account? {
        return persistedUsers.firstOrNull { it.clients.contains(client) }?.account
    }

    fun findClientsByAccount(account: Account): Set<ChatClient>? {
        return persistedUsers.firstOrNull { it.account == account }?.clients
    }

    fun clientDisconnected(client: ChatClient) {
        val persistedConnection = persistedUsers.firstOrNull { it.clients.contains(client) } ?: return
        persistedConnection.clients.remove(client)

        if (persistedConnection.clients.isEmpty())
            persistedUsers.remove(persistedConnection)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}