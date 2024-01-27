package ua.mezik.socketchat.logic.services

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.mezik.socketchat.logic.repositories.ChatMessagesRepo
import ua.mezik.socketchat.logic.repositories.ChatsRepo
import ua.mezik.socketchat.messages.requests.*
import ua.mezik.socketchat.messages.responses.ChatResponse
import ua.mezik.socketchat.messages.responses.DeleteChatResponse
import ua.mezik.socketchat.models.Account
import ua.mezik.socketchat.models.Chat
import ua.mezik.socketchat.models.ChatMessage
import ua.mezik.socketchat.models.MessageType
import ua.mezik.socketchat.models.messages.FileMessage
import ua.mezik.socketchat.models.messages.TextMessage
import ua.mezik.socketchat.TransactionEither
import ua.mezik.socketchat.Transactions


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

        val participants = ArrayList<Account>()
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

            val chatUpdateResponse = ChatResponse.fromChat(targetChat)
            connectionsManager.sendTransactionToClients(targetChat.participants, chatUpdateResponse)
        }

        return Either.Right(chatMessage)
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
                ?: return Either.Left(Transactions.notFound(request.type))

        if (message.owner != account) return Transactions.accessDenied(request.type).left()

        val chat = message.chat
        messagesRepo.delete(message)

        return chat.right()
    }


    open fun fetchChats(request: FetchChatsRequest, account: Account): TransactionEither<Page<Chat>> {
        return Either.catch {
            chatsRepo.findAllByParticipantsContaining(account, PageRequest.of(request.page, request.limit))
        }
            .mapLeft { Transactions.serializeStatusResponse(it.localizedMessage, request.type, true) }
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

    open fun getMessageOrNull(messageId: Long): ChatMessage? =
        messagesRepo.findByIdOrNull(messageId)

    open fun getChatOrNull(chatId: Long): Chat? =
        chatsRepo.findByIdOrNull(chatId)

    open fun getChatIdsByParticipant(account: Account): List<Long> =
        chatsRepo.findAllIdsByOwnerId(account)

}