package com.mezik.uchat.service

import com.mezik.uchat.model.database.Account
import com.mezik.uchat.model.message.TransactionBase
import org.springframework.stereotype.Service

@Service
class TransactionBroadcaster(
    private val persistenceManager: PersistenceManager
) {
    fun broadcastToClients(accounts: Iterable<Account>, transactionSupplier: (Account) -> TransactionBase?) {
        val clientsMap = persistenceManager.findAccountsClientsAsMap(accounts)

        for ((account, clients) in clientsMap) {
            val transaction = transactionSupplier.invoke(account)
                ?: continue

            for (client in clients)
                client.sendToClient(transaction)
        }
    }

    fun sendToClient(account:Account, transaction: TransactionBase) {
        persistenceManager.findClientsByAccount(account)?.forEach { client ->
            client.sendToClient(transaction)
        }
    }

    fun broadcastToClients(accounts: Iterable<Account>, transaction: TransactionBase) {
        val clients = persistenceManager.findAccountsClients(accounts) ?: return

        for (client in clients)
            client.sendToClient(transaction)
    }

    fun broadcastToClientsExcept(accounts: List<Account>, exception: Account, transaction: TransactionBase) {
        val filteredAccounts = ArrayList<Account>(accounts)
        filteredAccounts.remove(exception)

        val clients = persistenceManager.findAccountsClients(filteredAccounts) ?: return

        for (client in clients)
            client.sendToClient(transaction)
    }
}