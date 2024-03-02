package com.mezik.uchat.service

import com.mezik.uchat.model.database.*
import com.mezik.uchat.model.message.*
import com.mezik.uchat.service.repository.ChatMessagesRepository
import com.mezik.uchat.service.repository.ChatsRepository
import com.mezik.uchat.service.results.*
import com.mezik.uchat.shared.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.stream.Collectors

@Service
class ChatService(
    private val accountsService: AccountsService,
    private val chatsRepository: ChatsRepository,
    private val messagesRepo: ChatMessagesRepository
) {
    fun createChat(request: CreateChatRequest, account: Account): Mono<Chat> {
        request.membersIds.add(account.id)

        return accountsService
            .findAccountsByIds(request.membersIds)
            .collect(Collectors.toList())
            .flatMap { accounts ->
                var encryptedSymmetric: EncryptedSymmetric? = request.encryptedChatSymmetric
                var participantsDecryptionKeys = request.membersDecryptionKeys

                if (participantsDecryptionKeys.isNullOrEmpty() || encryptedSymmetric == null) {
                    encryptedSymmetric = EncryptionUtils.generateSymmetricKey().encoded
                    participantsDecryptionKeys = ArrayList(request.membersIds.size)

                    for (participant in accounts) {
                        val publicKey = participant.publicKey.asPublicKey().getOrNull()
                            ?: continue

                        val decryptionKey = EncryptionUtils.encrypt(
                            encryptedSymmetric,
                            publicKey,
                            DEFAULT_CIPHER_ALGO
                        ).getOrNull() ?: continue

                        participantsDecryptionKeys.add(
                            MemberDecryptionKey(
                                0,
                                participant.id,
                                decryptionKey
                            )
                        )
                    }
                }

                chatsRepository.save(
                    Chat(
                        request.chatName,
                        account,
                        accounts,
                        participantsDecryptionKeys
                    )
                )
            }
    }

    fun createMessage(request: MessageRequest, account: Account): Mono<ChatMessage> {
        return chatsRepository.findById(request.chatId)
            .orNotFound("chat")
            .handle { chat, sink ->
                if (chat.members.contains(account))
                    sink.next(ChatMessage.createBasedOnRequest(request, account, chat))
                else
                    sink.error(CachedExceptions.accessDenied)
            }
            .flatMap { chatMessage ->
                messagesRepo.save(chatMessage)
            }
    }

    fun addMemberToChat(request: ChatAddMemberRequest, account: Account): Mono<ChatMemberAddedResult> {
        if (request.decryptionKeys.isEmpty())
            return Mono.error(ValueNotProvidedException("Chat admin not provided any decryption keys"))

        return chatsRepository
            .findById(request.chatId)
            .orNotFound("chat")
            .handle { chat, sink ->
                if (chat.owner == account)
                    sink.next(chat)
                else
                    sink.error(CachedExceptions.accessDenied)
            }
            .flatMap { chat ->
                accountsService.findAccount(request.memberId)
                    .orNotFound("account")
                    .map { account ->
                        ChatMemberAddedResult(account, chat)
                    }
            }
            .flatMap { (member, chat) ->
                val updatedChat = chat.copy(
                    members = chat.members + member,
                    membersDecryptionKeys = chat.membersDecryptionKeys + request.decryptionKeys
                )

                chatsRepository
                    .save(updatedChat)
                    .map {
                        ChatMemberAddedResult(member, it)
                    }
            }
    }

    fun editMessage(request: MessageEditRequest, account: Account): Mono<TextMessage> {
        return messagesRepo
            .findById(request.messageId)
            .orNotFound("message")
            .handle { message, sink ->
                if (message.owner != account)
                    sink.error(CachedExceptions.accessDenied)
                else if (message.type != MessageType.TEXT || message !is TextMessage)
                    sink.error(CachedExceptions.messageNotEditable)
                else
                    sink.next(message)
            }
            .cast(TextMessage::class.java)
            .flatMap { message ->
                val editedMessage = message.copy(text = request.buffer, isEdited = true)
                messagesRepo.save(editedMessage)
            }
    }

    fun deleteMessage(request: MessageDeleteRequest, account: Account): Mono<ChatMessageDeletedResult> {
        return messagesRepo
            .findById(request.messageId)
            .orNotFound("message")
            .handle { message, sink ->
                if (message.owner != account)
                    sink.error(CachedExceptions.accessDenied)
                else
                    sink.next(message)
            }
            .flatMap { message ->
                messagesRepo.delete(message)
                    .then(Mono.fromCallable { ChatMessageDeletedResult(message.chat, message.id) })
            }
    }

    fun fetchChats(request: FetchChatsRequest, account: Account): Flux<Chat> {
        return chatsRepository.findAllByMembersContaining(account, PageRequest.of(request.page, request.limit))
    }

    fun fetchChatMessages(
        request: FetchChatMessagesRequest,
        account: Account
    ): Flux<ChatMessage> {
        return chatsRepository.findById(request.chatId)
            .orNotFound("chat")
            .handle { chat, sink ->
                if (chat.members.contains(account))
                    sink.next(chat)
                else
                    sink.error(CachedExceptions.accessDenied)
            }
            .flatMapMany { chat ->
                messagesRepo.findAllByChat(
                    chat,
                    PageRequest.of(request.page, request.limit).withSort(Sort.by("timestamp").reverse())
                )
            }
    }

    fun deleteChat(
        request: DeleteChatRequest, account: Account
    ): Mono<ChatDeletedResult> {
        return chatsRepository.findById(request.chatId)
            .orNotFound("chat")
            .handle { chat, sink ->
                if (chat.owner != account)
                    sink.error(CachedExceptions.accessDenied)
                else
                    sink.next(chat)
            }
            .flatMap { chat ->
                val deletedResult = ChatDeletedResult(chat.id, chat.members)

                messagesRepo
                    .deleteAllByChat(chat)
                    .then(chatsRepository.delete(chat))
                    .then(Mono.just(deletedResult))
            }
    }

    fun fetchChatsByIds(request: FetchChatsByIdsRequest, account: Account): Flux<Chat> =
        chatsRepository.findAllByMembersContainingAndIdIsIn(account, request.chatsIds)

    fun getChatIdsByParticipant(account: Account): Flux<Long> =
        chatsRepository.findAllByMembersContaining(account).map(Chat::id)
}