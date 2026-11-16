package com.snowsplit.app.di

import android.content.Context
import androidx.room.Room
import com.snowsplit.app.data.local.db.DebtEntryDao
import com.snowsplit.app.data.local.db.PersonDao
import com.snowsplit.app.data.local.db.SnowSplitDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSnowSplitDatabase(
        @ApplicationContext context: Context
    ): SnowSplitDatabase = Room.databaseBuilder(
        context,
        SnowSplitDatabase::class.java,
        "snowsplit.db"
    ).build()

    @Provides
    fun providePersonDao(db: SnowSplitDatabase): PersonDao = db.personDao()

    @Provides
    fun provideDebtEntryDao(db: SnowSplitDatabase): DebtEntryDao = db.debtEntryDao()
}
