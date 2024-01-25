package ua.mezik.socketchat.logic.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ua.mezik.socketchat.ResultOrResponse
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
    open fun createMessage(request: MessageRequest, account: Account): ResultOrResponse<ChatMessage> {
        val targetChat = chatsRepo.findByIdOrNull(request.chatId)
            ?: return ResultOrResponse.failure("Chat not found", request.type)

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

        return ResultOrResponse.success(chatMessage)
    }

    @Transactional
    open fun editMessage(request: MessageEditRequest, account: Account): ResultOrResponse<TextMessage> {

        val message =
            messagesRepo.findByIdOrNull(request.messageId)
                ?: return ResultOrResponse.failure("Message ${request.messageId} not found", request.type)

        if (message.owner != account) return ResultOrResponse.accessDenied(request.type)

        if (message.type != MessageType.TEXT || message !is TextMessage)
            return ResultOrResponse.failure("Message not editable", request.type)

        message.text = request.buffer
        message.isEdited = true

        messagesRepo.save(message)

        return ResultOrResponse.success(message)
    }

    @Transactional
    open fun deleteMessage(request: MessageDeleteRequest, account: Account): ResultOrResponse<Chat> {
        val message =
            messagesRepo.findByIdOrNull(request.messageId)
                ?: return ResultOrResponse.failure("Message not found", request.type)

        if (message.owner != account) return ResultOrResponse.accessDenied(request.type)

        val chat = message.chat
        messagesRepo.delete(message)

        return ResultOrResponse.success(chat)
    }


    open fun fetchChats(request: FetchChatsRequest, account: Account): ResultOrResponse<Page<Chat>> {
        val chats =
            chatsRepo.findAllByParticipantsContaining(account, PageRequest.of(request.page, request.limit))
        return ResultOrResponse.success(chats)
    }

    open fun fetchChatMessages(
        request: FetchChatMessagesRequest,
        account: Account
    ): ResultOrResponse<Page<ChatMessage>> {

        val chat = chatsRepo.findByIdOrNull(request.chatId)
            ?: return ResultOrResponse.chatNotFound(request.type)

        if (!chat.participants.contains(account))
            return ResultOrResponse.accessDenied(request.type)

        val messages =
            messagesRepo.findAllByChat(
                chat,
                PageRequest.of(request.page, request.limit).withSort(Sort.by("timestamp").reverse())
            )

        return ResultOrResponse.success(messages)
    }

    @Transactional
    open fun deleteChat(
        request: DeleteChatRequest, account: Account
    ): ResultOrResponse<Pair<List<Account>, DeleteChatResponse>> {

        val chat = chatsRepo.findByIdOrNull(request.chatId)
            ?: return ResultOrResponse.chatNotFound(request.type)

        if (chat.owner != account) return ResultOrResponse.accessDenied(request.type)

        val chatId = chat.id
        val participants = chat.participants

        messagesRepo.deleteAllByChat(chat)
        chatsRepo.delete(chat)

        return ResultOrResponse.success(Pair(participants, DeleteChatResponse(chatId)))
    }

    open fun fetchChatsByIds(request: FetchChatsByIdsRequest, account: Account): ResultOrResponse<Iterable<Chat>> {
        val chats = chatsRepo.findAllByIdAndParticipantsContaining(request.chatsIds, account)
        return ResultOrResponse.success(chats)
    }

    open fun getMessageOrNull(messageId: Long): ChatMessage? {
        return messagesRepo.findByIdOrNull(messageId)
    }

    open fun getChatOrNull(chatId: Long): Chat? {
        return chatsRepo.findByIdOrNull(chatId)
    }

    open fun getChatIdsByParticipant(account: Account): List<Long> {
        return chatsRepo.findAllIdsByOwnerId(account)
    }

}