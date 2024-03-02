package com.mezik.uchat.service.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import com.mezik.uchat.model.database.Chat
import com.mezik.uchat.model.database.ChatMessage

@Repository
interface ChatMessagesRepository : ReactiveMongoRepository<ChatMessage, String> {
    fun findAllByChat(chat: Chat, pageable: Pageable): Flux<ChatMessage>
    fun deleteAllByChat(chat: Chat) : Mono<Void>
    fun findById(id:Long) : Mono<ChatMessage>
}
