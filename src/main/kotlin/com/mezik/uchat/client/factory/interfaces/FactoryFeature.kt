package com.mezik.uchat.client.factory.interfaces

import com.mixfa.bytebuddy_proxy.ClassInstanceBuilder
import com.mixfa.bytebuddy_proxy.MethodInterceptionDescription
import net.bytebuddy.dynamic.DynamicType


interface InstanceFieldsConfigurer {
    fun configureInstanceFields(fields: InstanceFields)

    @JvmInline
    value class InstanceFields(
        private val builder: ClassInstanceBuilder<*>
    ) {
        fun addField(fieldName: String, value: Any?) = apply {
            builder.withField(fieldName, value)
        }
    }
}

interface ClassConfigurer {
    fun configureClass(builder: DynamicType.Builder<*>): DynamicType.Builder<*>
    val interceptions: List<MethodInterceptionDescription>
}

interface FactoryFeature : InstanceFieldsConfigurer, ClassConfigurer