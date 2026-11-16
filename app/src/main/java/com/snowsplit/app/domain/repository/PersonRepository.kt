package com.snowsplit.app.domain.repository

import com.snowsplit.app.domain.model.PersonBubble
import kotlinx.coroutines.flow.Flow

interface PersonRepository {
    fun observePeople(): Flow<List<PersonBubble>>
    suspend fun getPersonById(id: Long): PersonBubble?
    suspend fun upsertPerson(person: PersonBubble): Long
    suspend fun deletePerson(personId: Long)
}
