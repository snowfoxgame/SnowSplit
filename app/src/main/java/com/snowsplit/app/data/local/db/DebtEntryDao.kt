package com.snowsplit.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtEntryDao {
    @Query("SELECT * FROM debt_entries ORDER BY occurredAt DESC, createdAt DESC, id DESC")
    fun observeAllEntries(): Flow<List<DebtEntryEntity>>

    @Query("SELECT * FROM debt_entries WHERE personId = :personId ORDER BY occurredAt DESC, createdAt DESC, id DESC")
    fun observeEntriesForPerson(personId: Long): Flow<List<DebtEntryEntity>>

    @Query("SELECT * FROM debt_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): DebtEntryEntity?

    @Insert
    suspend fun insert(entry: DebtEntryEntity): Long

    @Update
    suspend fun update(entry: DebtEntryEntity)

    @Insert
    suspend fun insertAll(entries: List<DebtEntryEntity>): List<Long>

    @Query("DELETE FROM debt_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)
}
