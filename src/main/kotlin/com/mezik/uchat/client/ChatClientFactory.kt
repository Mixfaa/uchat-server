package com.mezik.uchat.client

import com.mezik.uchat.client.rest.RestClient
import com.mezik.uchat.client.socket.SocketClient
import com.mezik.uchat.model.message.TransactionBase
import com.mezik.uchat.service.TransactionResolver
import com.mixfa.bytebuddy_proxy.ClassInstanceBuilder
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import com.mixfa.bytebuddy_proxy.ProxyClassMaker
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.net.Socket
import kotlin.reflect.jvm.javaMethod

class ChatClientFactoryConfigurationEvent(
    source: Any,
    private val interceptions: MutableList<MethodInterceptionDescription>
) : ApplicationEvent(source) {
    fun addInterception(interception: MethodInterceptionDescription) {
        interceptions.add(interception)
    }
}

typealias RestClientCallback = (RestClient, TransactionBase) -> Unit

object ChatClientFactory {
    @Component
    class Configurer(transactionResolver: TransactionResolver, eventPublisher: ApplicationEventPublisher) {
        init {
            configure(transactionResolver, eventPublisher)
        }
    }

    private lateinit var transactionResolver: TransactionResolver

    private lateinit var socketClientProxy: ClassInstanceBuilder<out SocketClient>
    private lateinit var restClientProxy: ClassInstanceBuilder<out RestClient>
    private lateinit var restClientWithCallbackProxy: ClassInstanceBuilder<out RestClient>

    @JvmStatic
    private fun callbackInvoker(obj: RestClient, callback: Any, arg: TransactionBase) {
        (callback as RestClientCallback)(obj, arg)
    }

    private fun configure(transactionResolver: TransactionResolver, eventPublisher: ApplicationEventPublisher) {
        this.transactionResolver = transactionResolver

        val interceptions = mutableListOf<MethodInterceptionDescription>()
        eventPublisher.publishEvent(ChatClientFactoryConfigurationEvent(this, interceptions)) // synchronous event

        // make socket client proxy class
        socketClientProxy =
            ClassInstanceBuilder(ProxyClassMaker.makeProxyClass(SocketClient::class.java, interceptions))
                .selectConstructor(Socket::class.java, TransactionResolver::class.java)

        // make rest client proxy class
        restClientProxy = ClassInstanceBuilder(ProxyClassMaker.makeProxyClass(RestClient::class.java, interceptions))
            .selectConstructor(SseEmitter::class.java, TransactionResolver::class.java)

        // make rest client proxy class with callback
        val callbackInterception = MethodInterceptionDescription.Builder()
            .matchers(
                ElementMatchers.isOverriddenFrom(ChatClient::class.java),
                ElementMatchers.takesArguments(TransactionBase::class.java)
            )
            .implementation(
                MethodCall
                    .invoke(::callbackInvoker.javaMethod!!)
                    .withThis()
                    .withField("_callback")
                    .withAllArguments()
            )
            .build()

        interceptions.add(callbackInterception)

        restClientWithCallbackProxy = ClassInstanceBuilder(
            ProxyClassMaker.makeProxyClass(RestClient::class.java, interceptions) {
                it.defineField("_callback", Any::class.java, Visibility.PUBLIC)
            })
            .selectConstructor(SseEmitter::class.java, TransactionResolver::class.java)
    }

    fun newSocketClient(socket: Socket): SocketClient {
        return socketClientProxy
            .newInstance(socket, transactionResolver)
    }

    fun newRestClient(emitter: SseEmitter): RestClient {
        return restClientProxy
            .newInstance(emitter, transactionResolver)
    }

    fun newRestClient(): RestClient {
        return restClientProxy
            .newInstance(SseEmitter(Long.MAX_VALUE), transactionResolver)
    }

    fun newRestClientWithCallback(callback: RestClientCallback): RestClient {
        val restClient = restClientWithCallbackProxy
            .clearFields()
            .withField("_callback", callback)
            .newInstance(SseEmitter(Long.MAX_VALUE), transactionResolver)
        return restClient
    }
}