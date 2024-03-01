package com.mezik.uchat.client.factory.features

import com.mezik.uchat.client.factory.interfaces.FactoryFeature
import com.mezik.uchat.client.factory.interfaces.InstanceFieldsConfigurer
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import net.bytebuddy.dynamic.DynamicType

class SimpleMethodInterception(interception: MethodInterceptionDescription) : FactoryFeature {
    override val interceptions: List<MethodInterceptionDescription> = listOf(interception)

    override fun configureInstanceFields(fields: InstanceFieldsConfigurer.InstanceFields) {}
    override fun configureClass(builder: DynamicType.Builder<*>): DynamicType.Builder<*> = builder
}