package ua.mezik.uchat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class SocketChat

fun main(args: Array<String>) {
    runApplication<SocketChat>(*args)
}