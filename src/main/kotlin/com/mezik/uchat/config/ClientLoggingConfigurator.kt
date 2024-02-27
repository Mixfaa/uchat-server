package com.mezik.uchat.config

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.client.ChatClientFactoryConfigurationEvent
import com.mezik.uchat.model.message.TransactionBase
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import kotlin.reflect.jvm.javaMethod

object ChatClientLogger {
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
class ClientLoggingConfigurator : ApplicationListener<ChatClientFactoryConfigurationEvent> {
    override fun onApplicationEvent(event: ChatClientFactoryConfigurationEvent) {
        val sendInterception = MethodInterceptionDescription.Builder()
            .matchers(
                ElementMatchers.named(ChatClient::sendToClient.name),
                ElementMatchers.isOverriddenFrom(ChatClient::class.java),
                ElementMatchers.hasSignature(
                    MethodDescription.ForLoadedMethod(ChatClient::sendToClient.javaMethod!!).asSignatureToken()
                )
            )
            .implementation(MethodCall.invoke(ChatClientLogger::interceptSend.javaMethod!!).withAllArguments())
            .build()

        val handleInterception = MethodInterceptionDescription.Builder()
            .matchers(
                ElementMatchers.named(ChatClient::handleTransaction.name),
                ElementMatchers.isOverriddenFrom(ChatClient::class.java),
                ElementMatchers.hasSignature(
                    MethodDescription.ForLoadedMethod(ChatClient::handleTransaction.javaMethod!!).asSignatureToken()
                )
            )
            .implementation(MethodCall.invoke(ChatClientLogger::interceptHandle.javaMethod!!).withAllArguments())
            .build()

        event.addInterception(sendInterception)
        event.addInterception(handleInterception)
    }
}