package com.mezik.uchat.service

import com.mezik.uchat.model.database.DatabaseIdSequence
import com.mezik.uchat.repository.DatabaseIdSeqRepository
import org.springframework.stereotype.Service

@Service
final class SequencedIdGenerator(
    private val databaseIdSeqRepository: DatabaseIdSeqRepository
) {
    init {
        instance = this
    }

    fun generateId(table: String): Long {
        val sequence = databaseIdSeqRepository.findById(table).block()
            ?: databaseIdSeqRepository.save(DatabaseIdSequence(table, 0)).block()!!

        val id = ++sequence.sequence

        databaseIdSeqRepository.save(sequence).subscribe()

        return id
    }

    companion object {
        lateinit var instance: SequencedIdGenerator
    }
}