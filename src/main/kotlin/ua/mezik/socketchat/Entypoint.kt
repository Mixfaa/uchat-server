package ua.mezik.socketchat

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ua.mezik.socketchat.handling.ClientHandler
import ua.mezik.socketchat.handling.TransactionsResolver
import java.net.InetAddress
import java.net.ServerSocket

@SpringBootApplication
open class SocketChat

fun main(args: Array<String>) {
    runApplication<SocketChat>(*args)
}