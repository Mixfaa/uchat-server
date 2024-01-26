package ua.mezik.socketchat.messages.requests

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import ua.mezik.socketchat.Utils
import ua.mezik.socketchat.messages.TransactionTypeIdResolver
import ua.mezik.socketchat.messages.responses.*
import kotlin.reflect.KClass

typealias SerializedTransaction = ByteArray

enum class TransactionType(val transactionClass: KClass<*>) {
    STATUS_RESPONSE(StatusResponse::class),
    HEARTBEAT(Heartbeat::class),
    REQUEST_LOGIN(LoginRequest::class),
    REQUEST_MESSAGE(MessageRequest::class),
    REQUEST_CREATE_CHAT(CreateChatRequest::class),
    REQUEST_FETCH_ACCOUNTS(FetchAccountsRequest::class),
    REQUEST_DELETE_MESSAGE(MessageDeleteRequest::class),
    REQUEST_EDIT_MESSAGE(MessageEditRequest::class),
    REQUEST_FETCH_CHATS(FetchChatsRequest::class),
    REQUEST_FETCH_CHAT_MESSAGES(FetchChatMessagesRequest::class),
    REQUEST_DELETE_CHAT(DeleteChatRequest::class),
    REQUEST_FETCH_ACCOUNTS_BY_IDS(FetchAccountsByIdsRequest::class),
    REQUEST_FETCH_CHATS_BY_IDS(FetchChatsByIdsRequest::class),

    RESPONSE_LOGIN(LoginResponse::class),
    RESPONSE_CHAT(ChatResponse::class),
    RESPONSE_DELETE_CHAT(DeleteChatResponse::class),
    RESPONSE_FETCH_ACCOUNTS(FetchAccountsResponse::class),
    RESPONSE_CHAT_MESSAGE(MessageResponse::class),
    RESPONSE_DELETE_MESSAGE(MessageDeleteResponse::class),
    RESPONSE_EDIT_MESSAGE(MessageEditResponse::class),
    RESPONSE_FETCH_CHATS(FetchChatsResponse::class),
    RESPONSE_FETCH_CHAT_MESSAGES(FetchChatMessagesResponse::class);

    companion object {
        fun fromTransactionClass(type: TransactionBase): TransactionType {
            return entries.first { it.transactionClass == type::class }
        }

        fun forValue(value: String): TransactionType {
            val ordinal = value.toIntOrNull()
            return if (ordinal != null)
                entries[ordinal]
            else
                valueOf(value.uppercase())
        }
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.CUSTOM,
    include = JsonTypeInfo.As.PROPERTY,
    visible = false, // important
    property = "transaction_type"
)
@JsonTypeIdResolver(TransactionTypeIdResolver::class)
abstract class TransactionBase {
    @get:JsonIgnore
    val type: TransactionType by lazy { TransactionType.fromTransactionClass(this@TransactionBase) }

    @get:JsonIgnore
    val serialized: ByteArray by lazy { Utils.serializeTransaction(this@TransactionBase) }
}
