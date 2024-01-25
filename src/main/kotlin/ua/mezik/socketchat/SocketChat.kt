package ua.mezik.socketchat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import ua.mezik.socketchat.messages.requests.TransactionType
import ua.mezik.socketchat.messages.responses.StatusResponse

@SpringBootApplication
open class SocketChat

fun main(args: Array<String>) {
    runApplication<SocketChat>(*args)
}