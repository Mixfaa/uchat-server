package com.mezik.uchat.client.factory

import com.mezik.uchat.client.factory.interfaces.FactoryFeature
import com.mezik.uchat.client.factory.interfaces.InstanceFields
import com.mezik.uchat.client.factory.interfaces.InstanceFieldsConfigurer
import com.mezik.uchat.client.rest.RestClient
import com.mezik.uchat.client.socket.SocketClient
import com.mezik.uchat.service.TransactionResolver
import com.mixfa.bytebuddy_proxy.ClassInstanceBuilder
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import com.mixfa.bytebuddy_proxy.ProxyClassMaker
import net.bytebuddy.dynamic.DynamicType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.net.Socket





interface ChatClientFactory {
    fun newSocketClient(socket: Socket): SocketClient
    fun newRestClient(emitter: SseEmitter): RestClient
    fun newRestClient(): RestClient
}

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

        val instanceFields = InstanceFields(socketClientBuilder)

        instanceConfigurators.forEach { feature ->
            feature.configureInstanceFields(instanceFields)
        }

        return socketClientBuilder.newInstance(socket, transactionResolver)
    }

    override fun newRestClient(emitter: SseEmitter): RestClient {
        restClientBuilder.clearFields()

        val instanceFields = InstanceFields(restClientBuilder)

        instanceConfigurators.forEach { feature ->
            feature.configureInstanceFields(instanceFields)
        }

        return restClientBuilder.newInstance(emitter, transactionResolver)
    }

    override fun newRestClient(): RestClient {
        return newRestClient(SseEmitter(Long.MAX_VALUE))
    }
}

class CustomizedChatClientFactory(
    private val restClientProxy: Class<out RestClient>,
    private val socketClientProxy: Class<out SocketClient>,
    private val instanceConfigurators: List<InstanceFieldsConfigurer>,

    private val transactionResolver: TransactionResolver
) : ChatClientFactory {
    override fun newSocketClient(socket: Socket): SocketClient {
        val instanceBuilder = ClassInstanceBuilder(socketClientProxy)
            .selectConstructor(Socket::class.java, TransactionResolver::class.java)

        val instanceFields = InstanceFields(instanceBuilder)

        instanceConfigurators.forEach { feature ->
            feature.configureInstanceFields(instanceFields)
        }

        return instanceBuilder.newInstance(socket, transactionResolver)
    }

    override fun newRestClient(emitter: SseEmitter): RestClient {
        val instanceBuilder = ClassInstanceBuilder(restClientProxy)
            .selectConstructor(SseEmitter::class.java, TransactionResolver::class.java)

        val instanceFields = InstanceFields(instanceBuilder)

        instanceConfigurators.forEach { feature ->
            feature.configureInstanceFields(instanceFields)
        }

        return instanceBuilder.newInstance(emitter, transactionResolver)
    }

    override fun newRestClient(): RestClient {
        return newRestClient(SseEmitter(Long.MAX_VALUE))
    }
}

@Component
class ChatClientFactoryBuilder(
    private val transactionResolver: TransactionResolver,
    eventPublisher: ApplicationEventPublisher
) {
    private final val configuredFeatures: List<FactoryFeature>

    private final val defaultFactory: DefaultChatClientFactory

    init {
        val features = mutableListOf<FactoryFeature>()
        val event = ChatClientFactoryConfigurationEvent(this, features)

        eventPublisher.publishEvent(event) // synchronous event

        this.configuredFeatures = features
        this.defaultFactory = setupDefaultFactory()
    }

    private fun <T> configureProxyClass(clazz: Class<T>, featuresList: List<FactoryFeature>): Class<out T> {
        val finalInterceptions = mutableListOf<MethodInterceptionDescription>()
        featuresList.forEach { feature ->
            finalInterceptions.addAll(feature.interceptions)
        }

        return ProxyClassMaker
            .makeProxyClass(clazz, finalInterceptions) { bb ->
                var builder = bb as DynamicType.Builder<*>

                featuresList.forEach { feature ->
                    builder = feature.configureClass(builder)
                }
                builder as DynamicType.Builder<out T>
            }
    }

    fun createFactory(vararg features: FactoryFeature): ChatClientFactory {
        val featuresList = mutableListOf(*features)
        featuresList.addAll(configuredFeatures)

        val restClientProxy = configureProxyClass(RestClient::class.java, featuresList)
        val socketClientProxy = configureProxyClass(SocketClient::class.java, featuresList)

        return CustomizedChatClientFactory(restClientProxy, socketClientProxy, features.asList(), transactionResolver)
    }

    fun defaultFactory(): ChatClientFactory {
        return defaultFactory
    }

    private fun setupDefaultFactory(): DefaultChatClientFactory {
        val restClientProxy = configureProxyClass(RestClient::class.java, configuredFeatures)
        val socketClientProxy = configureProxyClass(SocketClient::class.java, configuredFeatures)

        return DefaultChatClientFactory(restClientProxy, socketClientProxy, configuredFeatures, transactionResolver)
    }
}