package com.mezik.uchat.staticController

import com.mezik.uchat.model.message.TransactionUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/static")
class StaticController {
    @GetMapping("/transactions/schema")
    fun getTransactionsSchema(): String {
        return TransactionUtils.schema
    }

    @GetMapping("/transactions/schema/md5")
    fun getTransactionsSchemaMD5(): String {
        return TransactionUtils.schemaMD5Hash
    }
}