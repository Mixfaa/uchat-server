package ua.mezik.uchat.services

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.mezik.uchat.repositories.ChatMessagesRepo
import ua.mezik.uchat.repositories.ChatsRepo
import ua.mezik.uchat.model.Account
import ua.mezik.uchat.model.Chat
import ua.mezik.uchat.model.ChatMessage
import ua.mezik.uchat.model.MessageType
import ua.mezik.uchat.model.message.FileMessage
import ua.mezik.uchat.model.message.TextMessage
import ua.mezik.uchat.misc.TransactionEither
import ua.mezik.uchat.misc.Transactions
import ua.mezik.uchat.misc.mapLeftToStatusResponseFor
import ua.mezik.uchat.model.message.requests.*
import ua.mezik.uchat.model.message.responses.*


@Service
@Transactional
open class ChatService(
    private val accountsService: AccountsService,
    private val connectionsManager: ConnectionsManager,
    private val chatsRepo: ChatsRepo,
    private val messagesRepo: ChatMessagesRepo
) {
    @Transactional
    open fun createChat(chatName: String, participantsIds: List<Long>?, account: Account): Chat {

        val participants = mutableListOf<Account>()
        if (participantsIds != null)
            participants.addAll(accountsService.findAccountsByIds(participantsIds))

        participants.add(account)

        val chat = Chat(chatName, account, participants)
        chatsRepo.save(chat)

        return chat
    }

    @Transactional
    open fun createMessage(request: MessageRequest, account: Account): TransactionEither<ChatMessage> {
        val targetChat = chatsRepo.findByIdOrNull(request.chatId)
            ?: return Transactions.notFound(request.type).left()

        // maybe I should use some factory, but 3 lines of code
        val chatMessage: ChatMessage = when (request.messageType) {
            MessageType.TEXT -> TextMessage(account, targetChat, request.messageType, request.buffer)
            MessageType.FILE -> FileMessage(account, targetChat, request.messageType, request.buffer)
        }
        messagesRepo.save(chatMessage)

        if (targetChat.firstMessageId == -1L) {
            targetChat.firstMessageId = chatMessage.id
            chatsRepo.save(targetChat)

            val chatUpdateResponse = ChatResponse(targetChat)
            connectionsManager.sendTransactionToClients(targetChat.participants, chatUpdateResponse)
        }

        return chatMessage.right()
    }

    @Transactional
    open fun editMessage(request: MessageEditRequest, account: Account): TransactionEither<TextMessage> {

        val message =
            messagesRepo.findByIdOrNull(request.messageId)
                ?: return Transactions.notFound(request.type).left()

        if (message.owner != account) return Transactions.accessDenied(request.type).left()

        if (message.type != MessageType.TEXT || message !is TextMessage)
            return Transactions.serializeStatusResponse("Message not editable", request.type, true).left()

        message.text = request.buffer
        message.isEdited = true

        messagesRepo.save(message)

        return message.right()
    }

    @Transactional
    open fun deleteMessage(request: MessageDeleteRequest, account: Account): TransactionEither<Chat> {
        val message =
            messagesRepo.findByIdOrNull(request.messageId)
                ?: return Transactions.notFound(request.type).left()

        if (message.owner != account) return Transactions.accessDenied(request.type).left()

        val chat = message.chat
        messagesRepo.delete(message)

        return chat.right()
    }

    open fun fetchChats(request: FetchChatsRequest, account: Account): TransactionEither<Page<Chat>> {
        return Either.catch {
            chatsRepo.findAllByParticipantsContaining(account, PageRequest.of(request.page, request.limit))
        }.mapLeftToStatusResponseFor(request)
    }

    open fun fetchChatMessages(
        request: FetchChatMessagesRequest,
        account: Account
    ): TransactionEither<Page<ChatMessage>> {

        val chat = chatsRepo.findByIdOrNull(request.chatId)
            ?: return Transactions.notFound(request.type).left()

        if (!chat.participants.contains(account))
            return Transactions.accessDenied(request.type).left()

        val messages =
            messagesRepo.findAllByChat(
                chat,
                PageRequest.of(request.page, request.limit).withSort(Sort.by("timestamp").reverse())
            )

        return messages.right()
    }

    @Transactional
    open fun deleteChat(
        request: DeleteChatRequest, account: Account
    ): TransactionEither<Pair<List<Account>, DeleteChatResponse>> {
        val chat = chatsRepo.findByIdOrNull(request.chatId)
            ?: return Transactions.notFound(request.type).left()

        if (chat.owner != account) return Transactions.accessDenied(request.type).left()

        val chatId = chat.id
        val participants = chat.participants

        messagesRepo.deleteAllByChat(chat)
        chatsRepo.delete(chat)

        return (participants to DeleteChatResponse(chatId)).right()
    }

    open fun fetchChatsByIds(request: FetchChatsByIdsRequest, account: Account): TransactionEither<Iterable<Chat>> =
        chatsRepo.findAllByIdAndParticipantsContaining(request.chatsIds, account).right()

    open fun getChatIdsByParticipant(account: Account): List<Long> =
        chatsRepo.findAllIdsByOwnerId(account)

}