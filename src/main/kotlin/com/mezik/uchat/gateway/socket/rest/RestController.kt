package com.mezik.uchat.gateway.socket.rest

import com.mezik.uchat.model.message.LoginRequest
import com.mezik.uchat.model.message.RegisterRequest
import com.mezik.uchat.model.message.TransactionBase
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.security.Principal

@RestController
@RequestMapping("/rest_interface")
class RestController(
    private val restClientsManager: RestClientsManager
) {
    @GetMapping("/auth", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun authAndGetEmitter(@RequestBody loginRequest: LoginRequest, principal: Principal): SseEmitter {
        val client = restClientsManager.authenticatedRestClient(loginRequest, principal)
        return client.emitter
    }

    @GetMapping("/register", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun registerAndGetEmitter(@RequestBody registerRequest: RegisterRequest): SseEmitter {
        val client = restClientsManager.selfRegisterRestClient(registerRequest)
        return client.emitter
    }

    @PostMapping("/request")
    fun handleRequest(@RequestBody transaction: TransactionBase, principal: Principal) {
        if (transaction is LoginRequest || transaction is RegisterRequest) return
        restClientsManager.handleTransactionFrom(transaction, principal)
    }
}