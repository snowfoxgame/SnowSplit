package com.snowsplit.app.data.repository

import com.snowsplit.app.data.local.db.DebtEntryDao
import com.snowsplit.app.data.local.db.toDomain
import com.snowsplit.app.data.local.db.toEntity
import com.snowsplit.app.domain.model.DebtEntry
import com.snowsplit.app.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineDebtRepository @Inject constructor(
    private val debtEntryDao: DebtEntryDao
) : DebtRepository {

    override fun observeAllEntries(): Flow<List<DebtEntry>> =
        debtEntryDao.observeAllEntries().map { list -> list.map { it.toDomain() } }

    override fun observeEntriesForPerson(personId: Long): Flow<List<DebtEntry>> =
        debtEntryDao.observeEntriesForPerson(personId).map { list -> list.map { it.toDomain() } }

    override suspend fun getEntryById(id: Long): DebtEntry? =
        debtEntryDao.getEntryById(id)?.toDomain()

    override suspend fun upsertEntry(entry: DebtEntry): Long {
        val entity = entry.toEntity()
        return if (entity.id == 0L) {
            debtEntryDao.insert(entity)
        } else {
            debtEntryDao.update(entity)
            entity.id
        }
    }

    override suspend fun insertEntries(entries: List<DebtEntry>): List<Long> {
        if (entries.isEmpty()) return emptyList()
        return debtEntryDao.insertAll(entries.map { it.toEntity() })
    }

    override suspend fun deleteEntry(entryId: Long) {
        debtEntryDao.deleteById(entryId)
    }
}
