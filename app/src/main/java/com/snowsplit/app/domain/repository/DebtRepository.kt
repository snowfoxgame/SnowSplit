package com.snowsplit.app.domain.repository

import com.snowsplit.app.domain.model.DebtEntry
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun observeAllEntries(): Flow<List<DebtEntry>>
    fun observeEntriesForPerson(personId: Long): Flow<List<DebtEntry>>
    suspend fun getEntryById(id: Long): DebtEntry?
    suspend fun upsertEntry(entry: DebtEntry): Long
    suspend fun insertEntries(entries: List<DebtEntry>): List<Long>
    suspend fun deleteEntry(entryId: Long)
}
