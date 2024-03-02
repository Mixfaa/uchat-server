package com.mezik.uchat.config

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.client.factory.ChatClientFactoryBuilder
import com.mezik.uchat.client.factory.features.SimpleMethodInterception
import com.mezik.uchat.model.message.TransactionBase
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import kotlin.reflect.jvm.javaMethod

internal object SendReceiveLogger {
    @JvmStatic
    fun interceptSend(transaction: TransactionBase) {
        logger.info("Send: $transaction")
    }

    @JvmStatic
    fun interceptHandle(transaction: TransactionBase) {
        logger.info("Receive: $transaction")
    }

    @JvmStatic
    private val logger = LoggerFactory.getLogger("Client logger")
}

@Component
class ChatClientLoggingConfigurator : ApplicationListener<ChatClientFactoryBuilder.ConfigurationEvent> {
    override fun onApplicationEvent(event: ChatClientFactoryBuilder.ConfigurationEvent) {
        val sendInterception = MethodInterceptionDescription.Builder()
            .setMatchers(
                ElementMatchers.named(ChatClient::sendToClient.name),
                ElementMatchers.isOverriddenFrom(ChatClient::class.java),
                ElementMatchers.hasSignature(
                    MethodDescription.ForLoadedMethod(ChatClient::sendToClient.javaMethod!!).asSignatureToken()
                )
            )
            .setImlp(MethodCall.invoke(SendReceiveLogger::interceptSend.javaMethod!!).withAllArguments())
            .build()

        val handleInterception = MethodInterceptionDescription.Builder()
            .setMatchers(
                ElementMatchers.named(ChatClient::handleTransaction.name),
                ElementMatchers.isOverriddenFrom(ChatClient::class.java),
                ElementMatchers.hasSignature(
                    MethodDescription.ForLoadedMethod(ChatClient::handleTransaction.javaMethod!!).asSignatureToken()
                )
            )
            .setImlp(MethodCall.invoke(SendReceiveLogger::interceptHandle.javaMethod!!).withAllArguments())
            .build()

        event.addFeature(SimpleMethodInterception(sendInterception))
        event.addFeature(SimpleMethodInterception(handleInterception))
    }
}