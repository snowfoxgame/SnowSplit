package com.snowsplit.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people WHERE isArchived = 0 ORDER BY createdAt ASC, id ASC")
    fun observePeople(): Flow<List<PersonBubbleEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getPersonById(id: Long): PersonBubbleEntity?

    @Insert
    suspend fun insert(person: PersonBubbleEntity): Long

    @Update
    suspend fun update(person: PersonBubbleEntity)

    @Query("DELETE FROM people WHERE id = :personId")
    suspend fun deleteById(personId: Long)
}
