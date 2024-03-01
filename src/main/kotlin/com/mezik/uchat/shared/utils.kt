package com.mezik.uchat.shared

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.model.message.PublicKey
import com.mezik.uchat.model.message.StatusResponse
import com.mezik.uchat.model.message.TransactionType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Key
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

fun StatusResponse.matchesByProps(msg: String, respFor: TransactionType): Boolean =
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

fun <T> Mono<T>.orNotFound(subject: String) =
    this.switchIfEmpty(Mono.error(NotFoundException(subject)))

fun <T> Flux<T>.orNotFound(subject: String) =
    this.switchIfEmpty(Mono.error(NotFoundException(subject)))

fun <T> Mono<T>.errorIfEmpty(exception: Throwable) =
    this.switchIfEmpty(Mono.error(exception))

fun <T> Flux<T>.errorIfEmpty(exception: Throwable) =
    this.switchIfEmpty(Mono.error(exception))