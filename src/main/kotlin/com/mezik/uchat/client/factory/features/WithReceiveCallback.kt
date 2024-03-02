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

@JvmInline
private value class ReceiveCallback(
    val callback: (ChatClient, TransactionBase) -> Unit
)

internal object ReceiveInterceptor {
    @JvmStatic
    fun interceptedReceive(obj: ChatClient, callback: Any?, transaction: TransactionBase) {
        if (callback != null && callback is ReceiveCallback)
            callback.callback(obj, transaction)
    }
}

class WithReceiveCallback(
    callback: (ChatClient, TransactionBase) -> Unit
) : FactoryFeature {
    private val receiveCallback = ReceiveCallback(callback)

    override val interceptions: List<MethodInterceptionDescription> = listOf(
        MethodInterceptionDescription.Builder()
            .setMatchers(
                ElementMatchers.isOverriddenFrom(ChatClient::class.java),
                ElementMatchers.named(ChatClient::handleTransaction.name),
                ElementMatchers.hasSignature(
                    MethodDescription.ForLoadedMethod(
                        ChatClient::handleTransaction.javaMethod!!
                    ).asSignatureToken()
                )
            )
            .setImlp(
                MethodCall.invoke(ReceiveInterceptor::interceptedReceive.javaMethod!!)
                    .withThis()
                    .withField("_receive_callback")
                    .withAllArguments()
            )
            .build()
    )

    override fun configureInstanceFields(fields: InstanceFieldsConfigurer.InstanceFields) {
        fields.addField("_receive_callback", receiveCallback)
    }

    override fun configureClass(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .defineField("_receive_callback", Any::class.java, Visibility.PUBLIC)
    }


}