package com.mezik.uchat.model.database

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("id_sequence")
data class DatabaseIdSequence(
    @Id val id: String,
    var sequence: Long
)