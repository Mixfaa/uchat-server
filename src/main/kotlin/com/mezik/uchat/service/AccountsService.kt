package com.mezik.uchat.service

import com.mezik.uchat.model.database.Account
import com.mezik.uchat.model.message.FetchAccountsByIdsRequest
import com.mezik.uchat.model.message.FetchAccountsRequest
import com.mezik.uchat.model.message.LoginRequest
import com.mezik.uchat.model.message.RegisterRequest
import com.mezik.uchat.repository.AccountsRepository
import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.shared.CachedExceptions
import com.mezik.uchat.shared.UsernameTakenException
import com.mezik.uchat.shared.asPublicKey
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class AccountsService(
    private val accountsRepository: AccountsRepository,
    private val persistenceManager: PersistenceManager,
    private val passwordEncoder: PasswordEncoder
) : UserDetailsService {
    fun findAccount(id: Long): Mono<Account> {
        return accountsRepository.findById(id)
    }

    fun findAccountsByIds(ids: Iterable<Long>): Flux<Account> {
        return accountsRepository.findAllByIdIsIn(ids.toMutableList())
    }

    fun fetchAccounts(fetchAccounts: FetchAccountsRequest): Flux<Account> {
        val pageRequest = PageRequest.of(fetchAccounts.page, fetchAccounts.limit)

        return accountsRepository.findAllByUsernameContainingIgnoreCase(
            fetchAccounts.query ?: "", pageRequest
        )
    }

    fun handleRegisterRequest(request: RegisterRequest, client: ChatClient): Mono<Account> {
        return accountsRepository
            .existsByUsername(request.username)
            .flatMap { exist ->
                if (exist)
                    return@flatMap Mono.error(UsernameTakenException("Username ${request.username} is already taken"))

                if (request.publicKey.asPublicKey().isFailure)
                    return@flatMap Mono.error(CachedExceptions.publicKeyNotProvided)

                val account = Account(request.username, passwordEncoder.encode(request.password), request.publicKey)
                return@flatMap accountsRepository.save(account)
            }
            .doOnSuccess { account ->
                persistenceManager.persistConnectionFrom(account, client)
            }
    }

    fun handleLogin(request: LoginRequest, client: ChatClient): Mono<Account> {
        return accountsRepository.findByUsername(request.username)
            .switchIfEmpty(Mono.error(CachedExceptions.emptyMono))
            .flatMap { account ->
                if (passwordEncoder.matches(request.password, account.password))
                    Mono.just(account)
                else
                    Mono.error(CachedExceptions.passwordNotMatch)
            }
            .doOnSuccess { account ->
                persistenceManager.persistConnectionFrom(account, client)
            }
    }

    fun fetchAccountsByIds(request: FetchAccountsByIdsRequest): Flux<Account> {
        return accountsRepository.findAllByIdIsIn(request.ids.toList())
    }

    override fun loadUserByUsername(username: String): UserDetails = accountsRepository.findByUsername(username).block()
        ?: throw UsernameNotFoundException("username")
}