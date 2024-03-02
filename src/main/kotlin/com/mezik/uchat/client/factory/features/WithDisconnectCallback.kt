package com.mezik.uchat.client.factory.features

import com.mezik.uchat.client.ChatClient
import com.mezik.uchat.client.factory.interfaces.FactoryFeature
import com.mezik.uchat.client.factory.interfaces.InstanceFieldsConfigurer
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import kotlin.reflect.jvm.javaMethod

private class DisconnectCallback(
    private val callback: (ChatClient) -> Unit
) : (ChatClient) -> Unit by callback

internal fun interceptedDisconnect(obj: ChatClient, callback: Any?) {
    if (callback != null && callback is DisconnectCallback)
        callback(obj)
}

class WithDisconnectCallback(
    callback: (ChatClient) -> Unit
) : FactoryFeature {
    private val disconnectCallback = DisconnectCallback(callback)

    override val interceptions: List<MethodInterceptionDescription> = listOf(
        MethodInterceptionDescription.Builder()
            .setMatchers(
                ElementMatchers.isOverriddenFrom(ChatClient::class.java),
                ElementMatchers.named(ChatClient::handleDisconnect.name),
                ElementMatchers.hasSignature(
                    MethodDescription.ForLoadedMethod(
                        ChatClient::handleDisconnect.javaMethod!!
                    ).asSignatureToken()
                )
            )
            .setImlp(
                MethodCall.invoke(::interceptedDisconnect.javaMethod!!)
                    .withThis()
                    .withField("_disconnect_callback")
            )
            .beforeSuper()
            .build()
    )

    override fun configureInstanceFields(fields: InstanceFieldsConfigurer.InstanceFields) {
        fields.addField("_disconnect_callback", disconnectCallback)
    }

    override fun configureClass(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .defineField("_disconnect_callback", Any::class.java, Visibility.PUBLIC)
    }
}