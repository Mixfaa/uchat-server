package ua.mezik.socketchat.services

import arrow.core.left
import arrow.core.right
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ua.mezik.socketchat.misc.TransactionEither
import ua.mezik.socketchat.misc.Transactions
import ua.mezik.socketchat.handling.ClientHandler
import ua.mezik.socketchat.repositories.AccountsRepo

import ua.mezik.socketchat.model.Account
import ua.mezik.socketchat.model.message.requests.*
import ua.mezik.socketchat.model.message.responses.*

@Service
class AccountsService(
    private val accountsRepo: AccountsRepo, private val persistenceManager: ConnectionsManager
) {
    fun findAccountsByIds(ids: List<Long>): MutableList<Account> {
        val accounts = ArrayList<Account>(ids.size)
        for (id in ids) {
            val account = accountsRepo.findById(id)
            if (account.isPresent) accounts.add(account.get())
        }
        return accounts
    }

    fun fetchAccounts(fetchAccounts: FetchAccountsRequest): FetchAccountsResponse {
        val pageRequest = PageRequest.of(fetchAccounts.page, fetchAccounts.limit)

        if (fetchAccounts.query == null) return FetchAccountsResponse(
            accountsRepo.findAll(pageRequest).toList(), null
        )

        return FetchAccountsResponse(
            accountsRepo.findAllByUsernameContainingIgnoreCase(
                fetchAccounts.query, pageRequest
            ).toList(), fetchAccounts.query
        )
    }

    fun handleLogin(request: LoginRequest, client: ClientHandler): TransactionEither<Account> {
        var account = accountsRepo.findByUsername(request.username)

        if (account == null) {
            account = Account(request.username, request.password)
            accountsRepo.save(account)
        } else {
            if (!account.password.equals(request.password)) return Transactions.serializeStatusResponse(
                "Password not match",
                request.type,
                true
            ).left()
        }
        persistenceManager.persistConnectionFrom(account, client)
        return account.right()
    }

    fun fetchAccountsByIds(request: FetchAccountsByIdsRequest): FetchAccountsResponse {
        val fetchedAccounts = accountsRepo.findAllById(request.ids)
        return FetchAccountsResponse(fetchedAccounts, null)
    }

}