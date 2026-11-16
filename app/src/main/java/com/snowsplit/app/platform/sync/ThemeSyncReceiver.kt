package com.snowsplit.app.platform.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receives `com.snowapps.action.APPEARANCE_SYNC` broadcasts from SnowDo's when signature
 * permission and loop-prevention checks pass.
 */
class ThemeSyncReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncReceiverEntryPoint {
        fun themeSyncManager(): ThemeSyncManager
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != PartnerAppConfig.SYNC_ACTION) return

        val pendingResult = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SyncReceiverEntryPoint::class.java
        )
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(SYNC_BROADCAST_TIMEOUT_MS) {
                    entryPoint.themeSyncManager().handleIncomingBroadcast(intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val SYNC_BROADCAST_TIMEOUT_MS = 7_000L
    }
}
