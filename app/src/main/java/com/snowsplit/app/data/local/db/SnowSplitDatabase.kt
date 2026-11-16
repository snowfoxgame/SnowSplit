package com.snowsplit.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PersonBubbleEntity::class,
        DebtEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SnowSplitDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun debtEntryDao(): DebtEntryDao
}
