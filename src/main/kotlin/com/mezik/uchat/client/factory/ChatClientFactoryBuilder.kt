package com.mezik.uchat.client.factory

import com.mezik.uchat.client.factory.interfaces.ChatClientFactory
import com.mezik.uchat.client.factory.interfaces.FactoryFeature
import com.mezik.uchat.client.rest.RestClient
import com.mezik.uchat.client.socket.SocketClient
import com.mezik.uchat.service.TransactionResolver
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import com.mixfa.bytebuddy_proxy.ProxyClassMaker
import net.bytebuddy.dynamic.DynamicType
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ChatClientFactoryBuilder(
    private val transactionResolver: TransactionResolver,
    eventPublisher: ApplicationEventPublisher
) {
    private final val configuredFeatures: List<FactoryFeature>
    private final val defaultFactory: ChatClientFactory

    class ConfigurationEvent(
        source: Any,
        private val features: MutableList<FactoryFeature>
    ) : ApplicationEvent(source) {
        fun addFeature(feature: FactoryFeature) = features.add(feature)
    }

    init {
        val features = mutableListOf<FactoryFeature>()
        val event = ConfigurationEvent(this, features)

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

    private fun setupDefaultFactory(): ChatClientFactory {
        val restClientProxy = configureProxyClass(RestClient::class.java, configuredFeatures)
        val socketClientProxy = configureProxyClass(SocketClient::class.java, configuredFeatures)

        return CustomizedChatClientFactory(restClientProxy, socketClientProxy, configuredFeatures, transactionResolver)
    }
}