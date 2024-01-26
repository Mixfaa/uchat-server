package ua.mezik.socketchat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ua.mezik.socketchat.messages.requests.Heartbeat
import ua.mezik.socketchat.messages.requests.SerializedTransaction
import ua.mezik.socketchat.messages.requests.TransactionBase
import ua.mezik.socketchat.messages.requests.TransactionType


object Utils {
    fun splitJsons(string: String): List<String> {
        return string.split("\n").filter { it.isNotBlank() || it.isNotEmpty() }.toList()
    }

    val jsonMapper = ObjectMapper().enable(SerializationFeature.WRITE_ENUMS_USING_INDEX).registerKotlinModule()

    fun serializeProps(props: Map<String, Any>): SerializedTransaction {
        return jsonMapper.writeValueAsBytes(props) + '\n'.code.toByte()
    }

    fun serializeTransaction(transaction: TransactionBase): SerializedTransaction {
        return jsonMapper.writeValueAsBytes(transaction) + '\n'.code.toByte()
    }
}

object Transactions {
    private val statusResponseProps = HashMap<String, Any>()
    val serializedHeartbeat = Utils.serializeTransaction(Heartbeat())

    fun userNotAuthenticated(responseFor: TransactionType): SerializedTransaction =
        serializeStatusResponse("User not authenticated", responseFor, true)

    fun accessDenied(responseFor: TransactionType): SerializedTransaction =
        serializeStatusResponse("Access denied", responseFor, true)

    fun serializeStatusResponse(msg: String, respFor: TransactionType, fail: Boolean): SerializedTransaction {
        statusResponseProps["responseFor"] = respFor
        statusResponseProps["message"] = msg
        statusResponseProps["fail"] = fail

        return Utils.serializeProps(statusResponseProps)
    }

}