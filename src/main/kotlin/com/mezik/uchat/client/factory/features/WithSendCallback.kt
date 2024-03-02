package com.mezik.uchat.client.factory.features

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.client.factory.interfaces.FactoryFeature
import com.mezik.uchat.client.factory.interfaces.InstanceFieldsConfigurer
import com.mezik.uchat.model.message.TransactionBase
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import kotlin.reflect.jvm.javaMethod

private class SendCallback(
    private val callback: (ChatClient, TransactionBase) -> Unit
) : (ChatClient, TransactionBase) -> Unit by callback

internal fun interceptedSend(obj: ChatClient, callback: Any?, transaction: TransactionBase) {
    if (callback != null && callback is SendCallback)
        callback(obj, transaction)
}

class WithSendCallback(
    callback: (ChatClient, TransactionBase) -> Unit
) : FactoryFeature {
    private val sendCallback = SendCallback(callback)

    override val interceptions: List<MethodInterceptionDescription> = listOf(
        MethodInterceptionDescription.Builder()
            .setMatchers(
                ElementMatchers.isOverriddenFrom(ChatClient::class.java),
                ElementMatchers.named(ChatClient::sendToClient.name),
                ElementMatchers.hasSignature(
                    MethodDescription.ForLoadedMethod(
                        ChatClient::sendToClient.javaMethod!!
                    ).asSignatureToken()
                )
            )
            .setImlp(
                MethodCall.invoke(::interceptedSend.javaMethod!!)
                    .withThis()
                    .withField("_send_callback")
                    .withAllArguments()
            )
            .build()
    )

    override fun configureInstanceFields(fields: InstanceFieldsConfigurer.InstanceFields) {
        fields.addField("_send_callback", sendCallback)
    }

    override fun configureClass(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .defineField("_send_callback", Any::class.java, Visibility.PUBLIC)
    }
}