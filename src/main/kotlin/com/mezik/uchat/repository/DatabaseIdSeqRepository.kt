package com.mezik.uchat.repository

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import com.mezik.uchat.model.database.DatabaseIdSequence

@Repository
interface DatabaseIdSeqRepository : ReactiveMongoRepository<DatabaseIdSequence, String>