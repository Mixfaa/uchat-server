package ua.mezik.socketchat.logic

import org.springframework.stereotype.Service
import ua.mezik.socketchat.SerializedTransaction
import ua.mezik.socketchat.Transactions
import ua.mezik.socketchat.logic.services.AccountsService
import ua.mezik.socketchat.logic.services.ChatService
import ua.mezik.socketchat.logic.services.ConnectionsManager
import ua.mezik.socketchat.messages.requests.*
import ua.mezik.socketchat.messages.responses.*
import ua.mezik.socketchat.models.Account

@Service
class TransactionsResolver(
    private val accountsService: AccountsService,
    private val chatService: ChatService,
    private val connectionsManager: ConnectionsManager,
) {
    fun clientDisconnected(client: ClientHandler) = connectionsManager.clientDisconnected(client)

    fun handleRequest(request: TransactionBase, clientHandler: ClientHandler): SerializedTransaction? {
        if (request is LoginRequest)
            return handleLoginRequest(request, clientHandler)

        val account = connectionsManager.accountFromClient(clientHandler)

        return when (request) {
            is CreateChatRequest -> createChat(request, account)

            is MessageRequest -> handleMessageRequest(request, account)
            is MessageEditRequest -> handleMessageEdit(request, account)
            is MessageDeleteRequest -> deleteMessage(request, account)

            is FetchChatMessagesRequest -> fetchChatMessages(request, account)
            is FetchChatsRequest -> fetchChats(request, account)
            is FetchChatsByIdsRequest -> fetchChatsByIds(request, account)
            is DeleteChatRequest -> deleteChat(request, account)

            is FetchAccountsRequest -> fetchAccounts(request).serialized
            is FetchAccountsByIdsRequest -> fetchAccountsByIds(request).serialized

            else -> StatusResponse("cant handle your request", TransactionType.fromTransactionClass(request)).serialized
        }
    }

    private fun handleLoginRequest(request: LoginRequest, client: ClientHandler): SerializedTransaction? {

        val (account, failResp) = accountsService.handleLogin(request, client)
        account ?: return failResp

        return LoginResponse(account, chatService.getChatIdsByParticipant(account)).serialized
    }

    private fun fetchAccounts(request: FetchAccountsRequest): TransactionBase = accountsService.fetchAccounts(request)

    private fun fetchAccountsByIds(request: FetchAccountsByIdsRequest): TransactionBase =
        accountsService.fetchAccountsByIds(request)

    private fun createChat(request: CreateChatRequest, account: Account?): SerializedTransaction {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        val chat = chatService.createChat(request.chatName, request.participantsIds, account)
        val chatResponse = ChatResponse.fromChat(chat)

        connectionsManager.sendTransactionToClientsExcept(chat.participants, account, chatResponse)

        return chatResponse.serialized
    }

    private fun handleMessageRequest(request: MessageRequest, account: Account?): SerializedTransaction? {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        val (message, failResp) = chatService.createMessage(request, account)
        message ?: return failResp

        val messageResponse = MessageResponse(
            message.id,
            account.id,
            message.chat.id,
            message.timestamp.time,
            request.messageType,
            request.buffer
        )

        connectionsManager.sendTransactionToClientsExcept(message.chat.participants, account, messageResponse)

        return messageResponse.serialized
    }

    private fun handleMessageEdit(request: MessageEditRequest, account: Account?): SerializedTransaction? {
        if (account == null) return Transactions.userNotAuthenticated(request.type)


        val (editResult, failResp) = chatService.editMessage(request, account)
        editResult ?: return failResp

        val editMessageResponse = MessageEditResponse.fromMessage(editResult)

        connectionsManager.sendTransactionToClientsExcept(editResult.chat.participants, account, editMessageResponse)

        return editMessageResponse.serialized
    }

    private fun deleteMessage(request: MessageDeleteRequest, account: Account?): SerializedTransaction? {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        val (chat, failResp) = chatService.deleteMessage(request, account)
        chat ?: return failResp

        val deleteMessageResponse = MessageDeleteResponse(request.messageId, chat.id)

        connectionsManager.sendTransactionToClientsExcept(chat.participants, account, deleteMessageResponse)

        return deleteMessageResponse.serialized
    }

    private fun fetchChatMessages(request: FetchChatMessagesRequest, account: Account?): SerializedTransaction? {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        val (messages, failResp) = chatService.fetchChatMessages(request, account)
        messages ?: return failResp

        return FetchChatMessagesResponse(
            request.chatId,
            messages.toList().map(MessageResponse::fromMessage)
        ).serialized
    }

    private fun fetchChats(request: FetchChatsRequest, account: Account?): SerializedTransaction? {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        val (chats, failResp) = chatService.fetchChats(request, account)
        chats ?: return failResp

        return FetchChatsResponse(chats.map(ChatResponse::fromChat).toList()).serialized
    }

    private fun fetchChatsByIds(request: FetchChatsByIdsRequest, account: Account?): SerializedTransaction? {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        val (chats, failResp) = chatService.fetchChatsByIds(request, account)
        chats ?: return failResp

        return FetchChatsResponse(chats.map(ChatResponse::fromChat)).serialized

    }

    private fun deleteChat(request: DeleteChatRequest, account: Account?): SerializedTransaction? {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        val (pair, failResp) = chatService.deleteChat(request, account)
        pair ?: return failResp

        val (participants, response) = pair

        connectionsManager.sendTransactionToClientsExcept(participants, account, response)
        return response.serialized
    }
}