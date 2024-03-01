package com.mezik.uchat.client.factory.interfaces

import com.mezik.uchat.client.rest.RestClient
import com.mezik.uchat.client.socket.SocketClient
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.net.Socket

interface ChatClientFactory {
    fun newSocketClient(socket: Socket): SocketClient
    fun newRestClient(emitter: SseEmitter): RestClient
    fun newRestClient(): RestClient
}