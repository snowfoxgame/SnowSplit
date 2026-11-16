package com.snowsplit.app.domain.repository

import kotlinx.coroutines.flow.Flow

enum class AccountStatus {
    LOCAL_ONLY
}

interface AuthRepository {
    fun observeAccountStatus(): Flow<AccountStatus>
}

interface SyncRepository {
    fun observeLastSyncTimestamp(): Flow<Long?>
}
