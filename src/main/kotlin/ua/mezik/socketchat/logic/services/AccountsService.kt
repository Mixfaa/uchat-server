package ua.mezik.socketchat.logic.services

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import ua.mezik.socketchat.ResultOrResponse
import ua.mezik.socketchat.logic.ClientHandler
import ua.mezik.socketchat.logic.repositories.AccountsRepo
import ua.mezik.socketchat.logic.repositories.ChatsRepo
import ua.mezik.socketchat.messages.requests.FetchAccountsByIdsRequest
import ua.mezik.socketchat.messages.requests.FetchAccountsRequest
import ua.mezik.socketchat.messages.requests.LoginRequest
import ua.mezik.socketchat.messages.responses.FetchAccountsResponse
import ua.mezik.socketchat.models.Account

@Service
class AccountsService(
    private val accountsRepo: AccountsRepo,
    private val chatsRepo: ChatsRepo,
    private val persistenceManager: ConnectionsManager
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

    fun handleLogin(request: LoginRequest, client: ClientHandler): ResultOrResponse<Account> {
        var account = accountsRepo.findByUsername(request.username)

        if (account == null) {
            account = Account(request.username, request.password)
            accountsRepo.save(account)
        } else {
            if (!account.password.equals(request.password))
                return ResultOrResponse.failure("Password not match", request.type)
        }

        val persistedConnection = persistenceManager.persistedConnectionByAccount(account)

        return if (persistedConnection != null) {
            println("login message from logined user")
            persistedConnection.clients.add(client)
            ResultOrResponse.success(account)
        } else {
            persistenceManager.addPersistedConnection(account, client)
            ResultOrResponse.success(account)
        }
    }

    fun fetchAccountsByIds(request: FetchAccountsByIdsRequest): FetchAccountsResponse {
        val fetchedAccounts = accountsRepo.findAllById(request.ids)
        return FetchAccountsResponse(fetchedAccounts, null)
    }

}