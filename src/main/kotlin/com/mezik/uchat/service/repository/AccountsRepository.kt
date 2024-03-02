package com.mezik.uchat.service.repository

import com.mezik.uchat.model.database.Account
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface AccountsRepository : ReactiveMongoRepository<Account, String> {
    fun findByUsername(username: String): Mono<Account>
    fun findAllByUsernameContainingIgnoreCase(username: String, page: Pageable): Flux<Account>
    fun findAllByIdIsIn(id: Collection<Long>): Flux<Account>
    fun findById(id:Long) : Mono<Account>
    fun existsByUsername(username: String) : Mono<Boolean>
}