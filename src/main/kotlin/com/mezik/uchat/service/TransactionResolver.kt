package com.mezik.uchat.service

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.model.message.*
import com.mezik.uchat.shared.CachedTransactions
import com.mezik.uchat.shared.sendErrorToClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class TransactionResolver(
    private val accountsService: AccountsService,
    private val chatService: ChatService,
    private val connectionsManager: PersistenceManager,
    private val transactionBroadcaster: TransactionBroadcaster
) {
    fun clientDisconnected(client: ChatClient) = connectionsManager.clientDisconnected(client)

    fun handleRequest(request: TransactionBase, client: ChatClient) {
        when (request) {
            is LoginRequest -> handleLoginRequest(request, client)
            is RegisterRequest -> handleRegisterRequest(request, client)

            is CreateChatRequest -> createChat(request, client)

            is MessageRequest -> handleMessageRequest(request, client)
            is MessageEditRequest -> handleMessageEdit(request, client)
            is MessageDeleteRequest -> deleteMessage(request, client)

            is FetchChatMessagesRequest -> fetchChatMessages(request, client)
            is FetchChatsRequest -> fetchChats(request, client)
            is FetchChatsByIdsRequest -> fetchChatsByIds(request, client)
            is DeleteChatRequest -> deleteChat(request, client)

            is FetchAccountsRequest -> fetchAccounts(request, client)
            is FetchAccountsByIdsRequest -> fetchAccountsByIds(request, client)

            is ChatAddMemberRequest -> addMemberToChat(request, client)

            else -> logUnhandledRequest(request, client)
        }
    }

    private fun handleRegisterRequest(request: RegisterRequest, client: ChatClient) {
        accountsService.handleRegisterRequest(request, client)
            .sendErrorToClient(request.type, client)
            .flatMap { account ->
                chatService
                    .getChatIdsByParticipant(account)
                    .collect(Collectors.toList())
                    .map { account to it }
            }
            .subscribe { (account, chats) ->
                client.sendToClient(LoginResponse(account, chats))
            }
    }

    private fun handleLoginRequest(request: LoginRequest, client: ChatClient) {
        accountsService.handleLogin(request, client)
            .sendErrorToClient(request.type, client)
            .flatMap { account ->
                chatService
                    .getChatIdsByParticipant(account)
                    .collect(Collectors.toList())
                    .map { account to it }
            }
            .subscribe { (account, participants) ->
                client.sendToClient(LoginResponse(account, participants))
            }
    }

    private fun addMemberToChat(request: ChatAddMemberRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .addMemberToChat(request, account)
            .sendErrorToClient(request.type, client)
            .subscribe { (member, chat) ->
                transactionBroadcaster.broadcastToClientsExcept(
                    chat.members,
                    member,
                    ChatAddMemberResponse(chat.id, member)
                )
                transactionBroadcaster.sendToClient(member, ChatResponse(chat, member.id))
            }
    }

    private fun fetchAccounts(request: FetchAccountsRequest, client: ChatClient) {
        accountsService.fetchAccounts(request)
            .collect(Collectors.toList())
            .subscribe { accounts ->
                client.sendToClient(FetchAccountsResponse(accounts, request.query))
            }
    }

    private fun fetchAccountsByIds(request: FetchAccountsByIdsRequest, client: ChatClient) {
        accountsService.fetchAccountsByIds(request)
            .collect(Collectors.toList())
            .subscribe { accounts ->
                client.sendToClient(FetchAccountsResponse(accounts, null))
            }
    }

    private fun createChat(request: CreateChatRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .createChat(request, account)
            .sendErrorToClient(request.type, client)
            .subscribe { chat ->
                transactionBroadcaster.broadcastToClients(chat.members) { account ->
                    ChatResponse(chat, account.id)
                }
            }
    }

    private fun handleMessageRequest(request: MessageRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .createMessage(request, account)
            .sendErrorToClient(request.type, client)
            .subscribe { message ->
                val messageResponse = MessageResponse(message)

                client.sendToClient(messageResponse)
                transactionBroadcaster.broadcastToClientsExcept(
                    message.chat.members,
                    account,
                    messageResponse
                )
            }
    }

    private fun handleMessageEdit(request: MessageEditRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .editMessage(request, account)
            .sendErrorToClient(request.type, client)
            .subscribe { textMessage ->
                val editMessageResponse = MessageEditResponse(textMessage)

                client.sendToClient(editMessageResponse)
                transactionBroadcaster.broadcastToClientsExcept(
                    textMessage.chat.members,
                    account,
                    editMessageResponse
                )
            }
    }

    private fun deleteMessage(request: MessageDeleteRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .deleteMessage(request, account)
            .sendErrorToClient(request.type, client)
            .subscribe { chat ->
                val deleteMessageResponse = MessageDeleteResponse(request.messageId, chat.id)

                client.sendToClient(deleteMessageResponse)
                transactionBroadcaster.broadcastToClientsExcept(chat.members, account, deleteMessageResponse)
            }
    }

    private fun fetchChatMessages(request: FetchChatMessagesRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .fetchChatMessages(request, account)
            .sendErrorToClient(request.type, client)
            .collect(Collectors.toList())
            .subscribe { messages ->
                client.sendToClient(
                    FetchChatMessagesResponse(
                        request.chatId, messages.map(::MessageResponse)
                    )
                )
            }
    }

    private fun fetchChats(request: FetchChatsRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .fetchChats(request, account)
            .sendErrorToClient(request.type, client)
            .collect(Collectors.toList())
            .subscribe { chats ->
                client.sendToClient(FetchChatsResponse(chats.map { ChatResponse(it, account.id) }))
            }
    }

    private fun fetchChatsByIds(request: FetchChatsByIdsRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .fetchChatsByIds(request, account)
            .sendErrorToClient(request.type, client)
            .collect(Collectors.toList())
            .subscribe { chats ->
                client.sendToClient(FetchChatsResponse(chats.map { ChatResponse(it, account.id) }))
            }
    }

    private fun deleteChat(request: DeleteChatRequest, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)
            ?: return client.sendToClient(CachedTransactions.userNotAuthenticated(request.type))

        chatService
            .deleteChat(request, account)
            .collect(Collectors.toList())
            .subscribe { accounts ->
                val response = DeleteChatResponse(request.chatId)
                client.sendToClient(response)
                transactionBroadcaster.broadcastToClientsExcept(accounts, account, response)
            }
    }

    private fun logUnhandledRequest(request: TransactionBase, client: ChatClient) {
        val account = connectionsManager.findClientAccount(client)

        if (account != null)
            logger.error("Unhandled transaction {} from {} ({})", request, account, client)
        else
            logger.error("Unhandled transaction {} from {}", request, client)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}