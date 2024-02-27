package com.mezik.uchat.repository

import com.mezik.uchat.model.database.Account
import com.mezik.uchat.model.database.Chat
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface ChatsRepository : ReactiveMongoRepository<Chat, String> {
    fun findAllByMembersContaining(participant: Account, pageable: Pageable): Flux<Chat>
    fun findById(id: Long): Mono<Chat>

    fun findByIdAndOwner(id: Long, owner: Account) : Mono<Chat>

    fun findAllByMembersContaining(owner: Account): Flux<Chat>

    fun findAllByMembersContainingAndIdIsIn(
        participant: Account,
        ids: Iterable<Long>,
    ): Flux<Chat>
}