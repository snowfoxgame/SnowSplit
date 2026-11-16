package com.snowsplit.app.di

import com.snowsplit.app.core.common.DefaultDispatcherProvider
import com.snowsplit.app.core.common.DispatcherProvider
import com.snowsplit.app.data.repository.OfflineDebtRepository
import com.snowsplit.app.data.repository.OfflinePersonRepository
import com.snowsplit.app.data.repository.OfflineSettingsRepository
import com.snowsplit.app.data.repository.PlaceholderAuthRepository
import com.snowsplit.app.data.repository.PlaceholderSyncRepository
import com.snowsplit.app.domain.repository.AuthRepository
import com.snowsplit.app.domain.repository.DebtRepository
import com.snowsplit.app.domain.repository.PersonRepository
import com.snowsplit.app.domain.repository.SettingsRepository
import com.snowsplit.app.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: OfflineSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPersonRepository(impl: OfflinePersonRepository): PersonRepository

    @Binds
    @Singleton
    abstract fun bindDebtRepository(impl: OfflineDebtRepository): DebtRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: PlaceholderAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: PlaceholderSyncRepository): SyncRepository
}
