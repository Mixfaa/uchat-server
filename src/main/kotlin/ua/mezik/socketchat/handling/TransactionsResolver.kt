package ua.mezik.socketchat.handling

import org.springframework.stereotype.Service
import ua.mezik.socketchat.misc.Transactions
import ua.mezik.socketchat.misc.foldRight
import ua.mezik.socketchat.services.AccountsService
import ua.mezik.socketchat.services.ChatService
import ua.mezik.socketchat.services.ConnectionsManager
import ua.mezik.socketchat.model.message.requests.*
import ua.mezik.socketchat.model.message.responses.*
import ua.mezik.socketchat.model.Account

@Service
class TransactionsResolver(
    private val accountsService: AccountsService,
    private val chatService: ChatService,
    private val connectionsManager: ConnectionsManager,
) {
    fun clientDisconnected(client: ChatClient) = connectionsManager.clientDisconnected(client)

    fun handleRequest(request: TransactionBase, client: ChatClient): SerializedTransaction? {
        if (request is LoginRequest) return handleLoginRequest(request, client)

        val account = connectionsManager.accountFromClient(client)

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

            else -> StatusResponse("cant handle your request", request.type).serialized
        }
    }

    private fun handleLoginRequest(request: LoginRequest, client: ChatClient): SerializedTransaction {
        return accountsService.handleLogin(request, client).foldRight { account ->
            LoginResponse(account, chatService.getChatIdsByParticipant(account)).serialized
        }
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

    private fun handleMessageRequest(request: MessageRequest, account: Account?): SerializedTransaction {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        return chatService.createMessage(request, account)
            .foldRight { message ->
                val messageResponse = MessageResponse(
                    message.id,
                    account.id,
                    message.chat.id,
                    message.timestamp.time,
                    request.messageType,
                    request.buffer
                )

                connectionsManager.sendTransactionToClientsExcept(
                    message.chat.participants,
                    account,
                    messageResponse
                )

                messageResponse.serialized
            }
    }

    private fun handleMessageEdit(request: MessageEditRequest, account: Account?): SerializedTransaction {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        return chatService.editMessage(request, account)
            .foldRight { editResult ->
                val editMessageResponse = MessageEditResponse.fromMessage(editResult)

                connectionsManager.sendTransactionToClientsExcept(
                    editResult.chat.participants,
                    account,
                    editMessageResponse
                )

                editMessageResponse.serialized
            }
    }

    private fun deleteMessage(request: MessageDeleteRequest, account: Account?): SerializedTransaction {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        return chatService.deleteMessage(request, account)
            .foldRight { chat ->
                val deleteMessageResponse = MessageDeleteResponse(request.messageId, chat.id)

                connectionsManager.sendTransactionToClientsExcept(chat.participants, account, deleteMessageResponse)

                deleteMessageResponse.serialized
            }
    }

    private fun fetchChatMessages(request: FetchChatMessagesRequest, account: Account?): SerializedTransaction {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        return chatService.fetchChatMessages(request, account)
            .foldRight { messages ->
                FetchChatMessagesResponse(
                    request.chatId, messages.toList().map(MessageResponse::fromMessage)
                ).serialized
            }
    }

    private fun fetchChats(request: FetchChatsRequest, account: Account?): SerializedTransaction {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        return chatService.fetchChats(request, account)
            .foldRight { chats -> FetchChatsResponse(chats.map(ChatResponse::fromChat).toList()).serialized }
    }

    private fun fetchChatsByIds(request: FetchChatsByIdsRequest, account: Account?): SerializedTransaction {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        return chatService.fetchChatsByIds(request, account)
            .foldRight { chats -> FetchChatsResponse(chats.map(ChatResponse::fromChat)).serialized }
    }

    private fun deleteChat(request: DeleteChatRequest, account: Account?): SerializedTransaction {
        if (account == null) return Transactions.userNotAuthenticated(request.type)

        return chatService.deleteChat(request, account)
            .foldRight { (participants, response) ->
                connectionsManager.sendTransactionToClientsExcept(participants, account, response)
                response.serialized
            }
    }
}