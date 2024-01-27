package ua.mezik.socketchat.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import ua.mezik.socketchat.model.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ua.mezik.socketchat.model.Chat

@Repository
interface ChatMessagesRepo : JpaRepository<ChatMessage, Long> {
    fun findAllByChat(chat: Chat, pageable: Pageable) : Page<ChatMessage>
    fun deleteAllByChat(chat: Chat)
}
