package com.snowsplit.app.feature.account

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowsplit.app.domain.repository.AccountStatus
import com.snowsplit.app.domain.repository.AuthRepository
import com.snowsplit.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class AccountUiState(
    val status: AccountStatus = AccountStatus.LOCAL_ONLY,
    val lastSyncTimestamp: Long? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    authRepository: AuthRepository,
    syncRepository: SyncRepository
) : ViewModel() {
    val uiState: StateFlow<AccountUiState> = combine(
        authRepository.observeAccountStatus(),
        syncRepository.observeLastSyncTimestamp()
    ) { status, lastSync ->
        AccountUiState(status = status, lastSyncTimestamp = lastSync)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountUiState()
    )
}
