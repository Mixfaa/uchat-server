package com.mezik.uchat.client.factory

import com.mezik.uchat.client.factory.interfaces.InstanceFieldsConfigurer
import com.mezik.uchat.client.rest.RestClient
import com.mezik.uchat.client.socket.SocketClient
import com.mezik.uchat.service.TransactionResolver
import com.mixfa.bytebuddy_proxy.ClassInstanceBuilder
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.net.Socket

class DefaultChatClientFactory(
    restClientProxy: Class<out RestClient>,
    socketClientProxy: Class<out SocketClient>,
    private val instanceConfigurators: List<InstanceFieldsConfigurer>,

    private val transactionResolver: TransactionResolver
) : ChatClientFactory {
    private val socketClientBuilder = ClassInstanceBuilder(socketClientProxy)
        .selectConstructor(Socket::class.java, TransactionResolver::class.java)

    private val restClientBuilder = ClassInstanceBuilder(restClientProxy)
        .selectConstructor(SseEmitter::class.java, TransactionResolver::class.java)

    override fun newSocketClient(socket: Socket): SocketClient {
        socketClientBuilder.clearFields()

        val instanceFields = InstanceFieldsConfigurer.InstanceFields(socketClientBuilder)

        instanceConfigurators.forEach { feature ->
            feature.configureInstanceFields(instanceFields)
        }

        return socketClientBuilder.newInstance(socket, transactionResolver)
    }

    override fun newRestClient(emitter: SseEmitter): RestClient {
        restClientBuilder.clearFields()

        val instanceFields = InstanceFieldsConfigurer.InstanceFields(restClientBuilder)

        instanceConfigurators.forEach { feature ->
            feature.configureInstanceFields(instanceFields)
        }

        return restClientBuilder.newInstance(emitter, transactionResolver)
    }

    override fun newRestClient(): RestClient {
        return newRestClient(SseEmitter(Long.MAX_VALUE))
    }
}