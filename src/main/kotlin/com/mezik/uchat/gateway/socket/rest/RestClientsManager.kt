package com.mezik.uchat.gateway.socket.rest

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.client.factory.ChatClientFactoryBuilder
import com.mezik.uchat.client.factory.features.WithReceiveCallback
import com.mezik.uchat.client.factory.features.WithSendCallback
import com.mezik.uchat.client.rest.RestClient
import com.mezik.uchat.model.message.*
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import java.security.Principal
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

@Service
class RestClientsManager(
    factoryBuilder: ChatClientFactoryBuilder
) {
    private val restClients = Collections.synchronizedMap(HashMap<String, RestClient>())
    private val selfRegisterClients = CopyOnWriteArrayList<RestClient>()

    private val defaultFactory = factoryBuilder.defaultFactory()

    private val selfRegisterFactory = factoryBuilder
        .createFactory(
            WithSendCallback(::sendReceiveCallback),
            WithReceiveCallback(::sendReceiveCallback)
        )

    private fun sendReceiveCallback(client: ChatClient, transaction: TransactionBase) {
        when (client) {
            is RestClient -> {
                when (transaction) {
                    is LoginResponse -> {
                        restClients[transaction.user.username] = defaultFactory.newRestClient(client.emitter)
                        selfRegisterClients.remove(client)
                    }

                    is StatusResponse -> {
                        if (transaction.responseFor == TransactionType.REQUEST_REGISTER) {
                            client.emitter.complete()
                            selfRegisterClients.remove(client)
                        }
                    }
                    else -> {}
                }
            }

            else -> {}
        }
    }


    fun selfRegisterRestClient(registerRequest: RegisterRequest): RestClient {
        val selfRegisterClient = selfRegisterFactory.newRestClient()

        coroutineScope.launch {
            delay(500)
            selfRegisterClient.handleTransaction(registerRequest)
        }

        selfRegisterClients.add(selfRegisterClient)
        return selfRegisterClient
    }

    fun authenticatedRestClient(loginRequest: LoginRequest, principal: Principal): RestClient =
        restClients.getOrPut(principal.name) {
            val client = selfRegisterFactory.newRestClient()

            coroutineScope.launch {
                delay(500)
                client.handleTransaction(loginRequest)
            }

            client
        }

    fun handleTransactionFrom(transaction: TransactionBase, client: ChatClient) {
        client.handleTransaction(transaction)
    }

    fun handleTransactionFrom(transaction: TransactionBase, principal: Principal) {
        val client = restClients[principal.name] ?: return
        client.handleTransaction(transaction)
    }

    companion object {
        private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }
}