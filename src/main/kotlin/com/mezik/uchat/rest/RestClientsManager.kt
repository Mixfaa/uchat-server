package com.mezik.uchat.rest

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.client.ChatClientFactory
import com.mezik.uchat.client.rest.RestClient
import com.mezik.uchat.model.message.*
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import java.security.Principal
import java.util.*

@Service
class RestClientsManager {
    private val restClientsImpl = Collections.synchronizedMap(HashMap<String, RestClient>())
    private val selfRegisterClients = mutableListOf<RestClient>()

    fun selfRegisterRestClient(registerRequest: RegisterRequest): RestClient {
        val selfRegisterClient = ChatClientFactory.newRestClientWithCallback { client, transaction ->
            when (transaction) {
                is LoginResponse -> {
                    restClientsImpl[transaction.user.username] = ChatClientFactory.newRestClient(client.emitter)
                    selfRegisterClients.remove(client)
                }

                is StatusResponse ->
                    if (transaction.responseFor == TransactionType.REQUEST_REGISTER) {
                        client.emitter.complete()
                        selfRegisterClients.remove(client)
                    }

                else -> {}
            }
        }

        coroutineScope.launch {
            delay(500)
            selfRegisterClient.handleTransaction(registerRequest)
        }

        selfRegisterClients.add(selfRegisterClient)
        return selfRegisterClient
    }

    fun authenticatedRestClient(loginRequest: LoginRequest, principal: Principal): RestClient =
        restClientsImpl.getOrPut(principal.name) {
            val client = ChatClientFactory.newRestClient()

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
        val client = restClientsImpl[principal.name] ?: return
        client.handleTransaction(transaction)
    }

    companion object {
        private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }
}