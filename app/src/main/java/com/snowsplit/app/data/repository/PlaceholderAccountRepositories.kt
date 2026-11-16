package com.snowsplit.app.data.repository

import com.snowsplit.app.domain.repository.AccountStatus
import com.snowsplit.app.domain.repository.AuthRepository
import com.snowsplit.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceholderAuthRepository @Inject constructor() : AuthRepository {
    override fun observeAccountStatus(): Flow<AccountStatus> = flowOf(AccountStatus.LOCAL_ONLY)
}

@Singleton
class PlaceholderSyncRepository @Inject constructor() : SyncRepository {
    override fun observeLastSyncTimestamp(): Flow<Long?> = flowOf(null)
}
