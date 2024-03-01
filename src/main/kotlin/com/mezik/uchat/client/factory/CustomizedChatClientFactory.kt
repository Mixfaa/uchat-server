package com.mezik.uchat.client.factory

import com.mezik.uchat.client.factory.interfaces.InstanceFieldsConfigurer
import com.mezik.uchat.client.rest.RestClient
import com.mezik.uchat.client.socket.SocketClient
import com.mezik.uchat.service.TransactionResolver
import com.mixfa.bytebuddy_proxy.ClassInstanceBuilder
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.net.Socket

class CustomizedChatClientFactory(
    private val restClientProxy: Class<out RestClient>,
    private val socketClientProxy: Class<out SocketClient>,
    private val instanceConfigurators: List<InstanceFieldsConfigurer>,

    private val transactionResolver: TransactionResolver
) : ChatClientFactory {
    override fun newSocketClient(socket: Socket): SocketClient {
        val instanceBuilder = ClassInstanceBuilder(socketClientProxy)
            .selectConstructor(Socket::class.java, TransactionResolver::class.java)

        val instanceFields = InstanceFieldsConfigurer.InstanceFields(instanceBuilder)

        instanceConfigurators.forEach { feature ->
            feature.configureInstanceFields(instanceFields)
        }

        return instanceBuilder.newInstance(socket, transactionResolver)
    }

    override fun newRestClient(emitter: SseEmitter): RestClient {
        val instanceBuilder = ClassInstanceBuilder(restClientProxy)
            .selectConstructor(SseEmitter::class.java, TransactionResolver::class.java)

        val instanceFields = InstanceFieldsConfigurer.InstanceFields(instanceBuilder)

        instanceConfigurators.forEach { feature ->
            feature.configureInstanceFields(instanceFields)
        }

        return instanceBuilder.newInstance(emitter, transactionResolver)
    }

    override fun newRestClient(): RestClient {
        return newRestClient(SseEmitter(Long.MAX_VALUE))
    }
}