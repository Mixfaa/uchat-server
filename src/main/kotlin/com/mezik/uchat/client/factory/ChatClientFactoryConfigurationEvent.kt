package com.mezik.uchat.client.factory

import com.mezik.uchat.client.factory.interfaces.FactoryFeature
import org.springframework.context.ApplicationEvent

class ChatClientFactoryConfigurationEvent(
    source: Any,
    private val features: MutableList<FactoryFeature>
) : ApplicationEvent(source) {
    fun addFeature(feature: FactoryFeature) {
        features.add(feature)
    }
}