package com.mezik.uchat.service

import com.mezik.uchat.model.database.*
import com.mezik.uchat.model.message.*
import com.mezik.uchat.repository.ChatMessagesRepository
import com.mezik.uchat.repository.ChatsRepository
import com.mezik.uchat.shared.*
import org.slf4j.LoggerFactory
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
            .switchIfEmpty(Mono.error(CachedExceptions.emptyMono))
            .flatMap { targetChat ->
                val chatMessage = ChatMessage.createBasedOnRequest(request, account, targetChat)
                return@flatMap Mono.just(chatMessage)
            }
            .flatMap { chatMessage ->
                messagesRepo.save(chatMessage)
            }
    }

    fun addMemberToChat(request: ChatAddMemberRequest, account: Account): Mono<Pair<Account, Chat>> {
        if (request.decryptionKeys.isEmpty())
            return Mono.error(ValueNotProvidedException("Chat admin not provided any decryption keys"))

        return chatsRepository
            .findById(request.chatId)
            .switchIfEmpty(Mono.error(CachedExceptions.emptyMono))
            .flatMap { chat ->
                if (chat.owner != account)
                    Mono.error(CachedExceptions.accessDenied)
                else
                    Mono.just(chat)
            }
            .flatMap { chat ->
                accountsService.findAccount(request.memberId).map { account ->
                    account to chat
                }
            }
            .flatMap { (member, chat) ->
                val updatedChat = chat.copy(
                    members = chat.members + member,
                    membersDecryptionKeys = chat.membersDecryptionKeys + request.decryptionKeys
                )

                chatsRepository.save(updatedChat)
                    .map { newChat -> member to newChat }
            }
    }

    fun editMessage(request: MessageEditRequest, account: Account): Mono<TextMessage> {
        return messagesRepo
            .findById(request.messageId)
            .switchIfEmpty(Mono.error(CachedExceptions.emptyMono))
            .flatMap { message ->
                if (message.owner != account)
                    Mono.error(CachedExceptions.accessDenied)
                else if (message.type != MessageType.TEXT || message !is TextMessage)
                    Mono.error(CachedExceptions.messageNotEditable)
                else
                    Mono.just(message)
            }
            .cast(TextMessage::class.java)
            .flatMap { message ->
                val editedMessage = message.copy(text = request.buffer, isEdited = true)
                messagesRepo.save(editedMessage)
            }
    }

    fun deleteMessage(request: MessageDeleteRequest, account: Account): Mono<Chat> {
        return messagesRepo
            .findById(request.messageId)
            .switchIfEmpty(Mono.error(CachedExceptions.emptyMono))
            .flatMap { message ->
                if (message.owner != account)
                    Mono.error(CachedExceptions.accessDenied)
                else
                    Mono.just(message)
            }
            .doOnSuccess { message ->
                messagesRepo.delete(message).subscribe()
            }
            .map { it.chat }
    }

    fun fetchChats(request: FetchChatsRequest, account: Account): Flux<Chat> {
        return chatsRepository.findAllByMembersContaining(account, PageRequest.of(request.page, request.limit))
    }

    fun fetchChatMessages(
        request: FetchChatMessagesRequest,
        account: Account
    ): Flux<ChatMessage> {
        return chatsRepository.findById(request.chatId)
            .switchIfEmpty(Mono.error(CachedExceptions.emptyMono))
            .flatMap { chat ->
                if (!chat.members.contains(account))
                    Mono.error(CachedExceptions.accessDenied)
                else
                    Mono.just(chat)
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
    ): Flux<Account> {
        return chatsRepository.findById(request.chatId)
            .switchIfEmpty(Mono.error(CachedExceptions.emptyMono))
            .flatMap { chat ->
                if (chat.owner != account)
                    Mono.error(CachedExceptions.accessDenied)
                else
                    Mono.just(chat)
            }
            .doOnSuccess { chat ->
                messagesRepo.deleteAllByChat(chat).subscribe()
                chatsRepository.delete(chat).subscribe()
            }
            .flatMapMany { chat -> Flux.fromIterable(chat.members) }
    }

    fun fetchChatsByIds(request: FetchChatsByIdsRequest, account: Account): Flux<Chat> =
        chatsRepository.findAllByMembersContainingAndIdIsIn(account, request.chatsIds)

    fun getChatIdsByParticipant(account: Account): Flux<Long> =
        chatsRepository.findAllByMembersContaining(account).map(Chat::id)

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}