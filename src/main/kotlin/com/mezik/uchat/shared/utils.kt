package com.mezik.uchat.shared

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.model.message.PublicKey
import com.mezik.uchat.model.message.StatusResponse
import com.mezik.uchat.model.message.TransactionType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

const val DEFAULT_CIPHER_ALGO = "RSA/ECB/PKCS1Padding"

object CachedTransactions {
    private val cachedTransactions = HashMap<StatusResponse, Int>()
    private const val MAX_CACHED_TRANSACTIONS = 75

    val passwordNotMath = getStatusResponse(
        "Password not match",
        TransactionType.REQUEST_LOGIN,
    )
    val invalidRequest = getStatusResponse(
        "Can`t parse your request",
        TransactionType.STATUS_RESPONSE,
    )

    fun userNotAuthenticated(responseFor: TransactionType): StatusResponse =
        getStatusResponse("User not authenticated", responseFor)

    fun getStatusResponse(msg: String, respFor: TransactionType): StatusResponse {
        var statusResponse = cachedTransactions
            .keys.find { it.matchesByProps(msg, respFor) }

        if (statusResponse == null) {
            statusResponse = StatusResponse(msg, respFor)
            cachedTransactions[statusResponse] = 1

            if (cachedTransactions.size >= MAX_CACHED_TRANSACTIONS) {
                val iterator = cachedTransactions.iterator()

                while (iterator.hasNext()) {
                    val (cachedStatusResponse, usages) = iterator.next()
                    if (cachedStatusResponse == statusResponse || usages > 5)
                        continue

                    iterator.remove()
                }
            }
        } else
            cachedTransactions.compute(statusResponse) { response, usages ->
                return@compute if (usages == null) 1 else usages + 1
            }

        return statusResponse
    }
}

object EncryptionUtils {
    private val symmetricKeyGenerator = KeyGenerator.getInstance("AES").also {
        it.init(256)
    }

    fun generateSymmetricKey(): SecretKey {
        return symmetricKeyGenerator.generateKey()
    }

    fun encrypt(data: ByteArray, key: Key, transformation: String): Result<ByteArray> = runCatching {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.doFinal(data)
    }

    fun decrypt(data: ByteArray, key: Key, transformation: String): Result<ByteArray> = runCatching {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, key)
        cipher.doFinal(data)
    }
}

private fun StatusResponse.matchesByProps(msg: String, respFor: TransactionType): Boolean =
    this.type == respFor && this.message == msg

fun Throwable.toStatusResponse(respFor: TransactionType): StatusResponse =
    when (this) {
        CachedExceptions.passwordNotMatch -> CachedTransactions.passwordNotMath
        else -> CachedTransactions.getStatusResponse(localizedMessage, respFor)
    }

fun <T> Mono<T>.sendErrorToClient(respFor: TransactionType, client: ChatClient): Mono<T> =
    this.onErrorComplete { error ->
        client.sendToClient(error.toStatusResponse(respFor))
        true
    }

fun <T> Flux<T>.sendErrorToClient(respFor: TransactionType, client: ChatClient): Flux<T> =
    this.onErrorComplete { error ->
        client.sendToClient(error.toStatusResponse(respFor))
        true
    }

fun PublicKey.asPublicKey(): Result<Key> = runCatching {
    KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(this))
}

fun PublicKey.asPrivateKey(): Result<Key> = runCatching {
    KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(this))
}