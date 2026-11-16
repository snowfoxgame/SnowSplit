package com.snowsplit.app.platform.sync

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.snowsplit.app.core.common.AppIconManager
import com.snowsplit.app.core.common.DispatcherProvider
import com.snowsplit.app.data.local.media.BackgroundImageStorage
import com.snowsplit.app.data.local.prefs.SettingsDataStore
import com.snowsplit.app.data.local.prefs.syncSignature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(FlowPreview::class)
@Singleton
class ThemeSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val backgroundImageStorage: BackgroundImageStorage,
    private val partnerAppDetector: PartnerAppDetector,
    private val appIconManager: AppIconManager,
    private val dispatcherProvider: DispatcherProvider
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    private val syncMutex = Mutex()
    private val localChangeTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    @Volatile
    private var started = false

    @Volatile
    private var isAppInForeground = false

    fun initialize() {
        if (started) return
        started = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        observeLocalAppearanceChanges()
        observeDebouncedPush()
        scope.launch {
            if (partnerAppDetector.refreshConnectionStatus()) {
                pullFromPartnerIfEnabled()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        scope.launch {
            if (partnerAppDetector.refreshConnectionStatus()) {
                pullFromPartnerIfEnabled()
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        isAppInForeground = true
        scope.launch {
            if (partnerAppDetector.refreshConnectionStatus()) {
                pullFromPartnerIfEnabled()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
        appIconManager.applyPendingIconIfNeeded()
    }

    fun onCrossAppSyncToggled(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            if (partnerAppDetector.refreshConnectionStatus()) {
                pullFromPartnerIfEnabled()
            }
        }
    }

    suspend fun handleIncomingBroadcast(intent: Intent) = withContext(dispatcherProvider.io) {
        if (intent.action != PartnerAppConfig.SYNC_ACTION) return@withContext

        if (!partnerAppDetector.refreshConnectionStatus()) return@withContext
        val currentState = settingsDataStore.readSyncState()
        if (!currentState.crossAppSyncEnabled) return@withContext

        val sourceExtra = intent.getStringExtra(PartnerAppConfig.EXTRA_SOURCE_PACKAGE)
        if (sourceExtra != null && sourceExtra != PartnerAppConfig.PARTNER_PACKAGE) {
            return@withContext
        }

        val inlinePayload = intent.getStringExtra(PartnerAppConfig.EXTRA_PAYLOAD_JSON)
            ?.let(SnowAppearanceSyncPayload::fromJson)

        val payload = inlinePayload ?: queryPartnerAppearancePayload() ?: return@withContext
        if (payload.sourcePackage != PartnerAppConfig.PARTNER_PACKAGE) return@withContext

        applyPayloadIfAccepted(payload)
    }

    suspend fun pullFromPartnerIfEnabled() = withContext(dispatcherProvider.io) {
        val state = settingsDataStore.readSyncState()
        if (!state.crossAppSyncEnabled) return@withContext
        if (!partnerAppDetector.refreshConnectionStatus()) return@withContext

        val payload = queryPartnerAppearancePayload() ?: return@withContext
        if (payload.sourcePackage != PartnerAppConfig.PARTNER_PACKAGE) return@withContext

        applyPayloadIfAccepted(payload)
    }

    private fun observeLocalAppearanceChanges() {
        scope.launch {
            var hasInitial = false
            var lastObservedSignature: String? = null
            settingsDataStore.syncStateFlow.collect { state ->
                val sig = state.currentSignature()
                if (!hasInitial) {
                    hasInitial = true
                    lastObservedSignature = sig
                    return@collect
                }
                if (sig == state.lastSyncedSignature) {
                    lastObservedSignature = sig
                    return@collect
                }
                if (sig != lastObservedSignature) {
                    lastObservedSignature = sig
                    if (state.crossAppSyncEnabled && state.partnerAppConnected) {
                        localChangeTrigger.tryEmit(Unit)
                    }
                }
            }
        }
    }

    private fun observeDebouncedPush() {
        scope.launch {
            localChangeTrigger
                .debounce(PUSH_DEBOUNCE_MS)
                .collect {
                    pushLocalAppearanceIfChanged()
                }
        }
    }

    private suspend fun pushLocalAppearanceIfChanged() = syncMutex.withLock {
        if (!partnerAppDetector.refreshConnectionStatus()) return@withLock
        val state = settingsDataStore.readSyncState()
        if (!state.crossAppSyncEnabled) return@withLock

        val currentSig = state.currentSignature()
        if (currentSig == state.lastSyncedSignature) return@withLock

        val managedFile = backgroundImageStorage.resolveManagedBackgroundFile(
            state.appearance.backgroundImageUri
        )
        val backgroundHash = if (managedFile != null) {
            state.localBackgroundHash?.takeIf { it.isNotBlank() }
                ?: backgroundImageStorage.computeFileSha256(managedFile)
        } else {
            null
        }
        val effectiveAppearance = state.appearance.copy(
            backgroundImageUri = if (managedFile != null) state.appearance.backgroundImageUri else null
        )
        val effectiveSig = effectiveAppearance.syncSignature(
            reduceLiveBlur = state.reduceLiveBlur,
            localBackgroundHash = backgroundHash
        )

        val now = System.currentTimeMillis()
        val revision = maxOf(now, state.lastSyncedRevision + 1L)
        val syncId = UUID.randomUUID().toString()

        settingsDataStore.recordOutgoingSync(
            syncId = syncId,
            revision = revision,
            localBackgroundHash = backgroundHash,
            signature = effectiveSig
        )

        val payload = SnowAppearanceSyncPayload.fromAppearance(
            sourcePackage = context.packageName,
            syncId = syncId,
            updatedAtEpochMillis = revision,
            appearance = effectiveAppearance,
            reduceLiveBlur = state.reduceLiveBlur,
            localBackgroundHash = backgroundHash
        )

        val intent = Intent(PartnerAppConfig.SYNC_ACTION).apply {
            setPackage(PartnerAppConfig.PARTNER_PACKAGE)
            putExtra(PartnerAppConfig.EXTRA_SOURCE_PACKAGE, payload.sourcePackage)
            putExtra(PartnerAppConfig.EXTRA_SYNC_ID, payload.syncId)
            putExtra(PartnerAppConfig.EXTRA_UPDATED_AT, payload.updatedAtEpochMillis)
            putExtra(PartnerAppConfig.EXTRA_PAYLOAD_JSON, payload.toJson())
        }
        context.sendBroadcast(intent, PartnerAppConfig.SYNC_PERMISSION)
    }

    private suspend fun applyPayloadIfAccepted(payload: SnowAppearanceSyncPayload) = syncMutex.withLock {
        val state = settingsDataStore.readSyncState()
        if (!state.crossAppSyncEnabled) return@withLock
        if (
            !SyncLoopGuard.shouldApply(
                payload = payload,
                ownPackage = context.packageName,
                lastAppliedSyncId = state.lastAppliedSyncId,
                lastSyncedRevision = state.lastSyncedRevision
            )
        ) {
            return@withLock
        }

        val existingFile = backgroundImageStorage.resolveManagedBackgroundFile(
            state.appearance.backgroundImageUri
        )

        val (resolvedBgUri, resolvedBgHash) = resolveBackgroundForIncomingPayload(
            payload = payload,
            existingUri = if (existingFile != null) state.appearance.backgroundImageUri else null,
            existingHash = state.localBackgroundHash,
            hasExistingFile = existingFile != null
        )

        val updatedAppearance = payload.toThemeAppearanceConfig(resolvedBackgroundUri = resolvedBgUri)
        val updatedSettings = settingsDataStore.applyRemoteAppearanceSync(
            appearance = updatedAppearance,
            reduceLiveBlur = payload.reduceLiveBlur,
            localBackgroundHash = resolvedBgHash,
            syncId = payload.syncId,
            updatedAtEpochMillis = payload.updatedAtEpochMillis
        )

        appIconManager.scheduleDesiredIcon(updatedSettings)
        if (!isAppInForeground) {
            appIconManager.applyPendingIconIfNeeded()
        }
        notifyWidgetsIfPresent()
    }

    private suspend fun resolveBackgroundForIncomingPayload(
        payload: SnowAppearanceSyncPayload,
        existingUri: String?,
        existingHash: String?,
        hasExistingFile: Boolean
    ): Pair<String?, String?> {
        if (!payload.hasCustomBackground) {
            backgroundImageStorage.clearStoredImages()
            return null to null
        }

        if (
            !SyncLoopGuard.shouldCopyBackground(
                hasCustomBackground = true,
                incomingHash = payload.customBackgroundHash,
                localBackgroundHash = existingHash,
                hasExistingLocalFile = hasExistingFile
            )
        ) {
            return existingUri to existingHash
        }

        val sourceUri = sanitizePartnerBackgroundUri(payload.customBackgroundContentUri)
        val copied = runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                backgroundImageStorage.saveSyncedBackground(stream)
            }
        }.getOrNull()?.getOrNull()

        return if (copied != null) {
            val (localUri, actualHash) = copied
            val effectiveHash = payload.customBackgroundHash?.takeIf { it.isNotBlank() } ?: actualHash
            localUri to effectiveHash
        } else {
            existingUri to existingHash
        }
    }

    private fun queryPartnerAppearancePayload(): SnowAppearanceSyncPayload? {
        return runCatching {
            context.contentResolver.query(
                PartnerAppConfig.PARTNER_APPEARANCE_URI,
                arrayOf(PartnerAppConfig.COLUMN_PAYLOAD_JSON),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val colIndex = cursor.getColumnIndex(PartnerAppConfig.COLUMN_PAYLOAD_JSON)
                if (colIndex < 0) return@use null
                val json = cursor.getString(colIndex) ?: return@use null
                SnowAppearanceSyncPayload.fromJson(json)
            }
        }.getOrNull()
    }

    private fun sanitizePartnerBackgroundUri(rawUri: String?): Uri {
        val parsed = rawUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        return if (
            parsed != null &&
            parsed.scheme == "content" &&
            parsed.authority == PartnerAppConfig.PARTNER_AUTHORITY
        ) {
            parsed
        } else {
            PartnerAppConfig.PARTNER_BACKGROUND_URI
        }
    }

    private fun notifyWidgetsIfPresent() {
        runCatching {
            val manager = AppWidgetManager.getInstance(context)
            // Hook ready for future SnowSplit home-screen AppWidgetProviders.
            manager.installedProviders
                .filter { it.provider.packageName == context.packageName }
                .forEach { provider ->
                    val ids = manager.getAppWidgetIds(provider.provider)
                    if (ids.isNotEmpty()) {
                        val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                            component = provider.provider
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                        context.sendBroadcast(updateIntent)
                    }
                }
        }
    }

    companion object {
        private const val PUSH_DEBOUNCE_MS = 400L
    }
}
