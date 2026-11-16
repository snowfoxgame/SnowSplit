package com.snowsplit.app.platform.sync

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import com.snowsplit.app.data.local.media.BackgroundImageStorage
import com.snowsplit.app.data.local.prefs.SettingsDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

/**
 * Exposes SnowSplit's active appearance configuration and custom background bytes exclusively
 * to the verified SnowDo's partner app.
 *
 * Uses lazy Hilt `@EntryPoint` lookup instead of `@AndroidEntryPoint` because
 * [ContentProvider.onCreate] runs before `Application.onCreate`.
 */
class ThemeSyncContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncProviderEntryPoint {
        fun settingsDataStore(): SettingsDataStore
        fun backgroundImageStorage(): BackgroundImageStorage
        fun partnerAppDetector(): PartnerAppDetector
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        val entryPoint = entryPoint(ctx)
        requireTrustedPartnerCaller(ctx, entryPoint)

        if (matchUri(ctx, uri) != CODE_APPEARANCE) {
            return null
        }

        val state = runBlocking { entryPoint.settingsDataStore().readSyncState() }
        val storage = entryPoint.backgroundImageStorage()
        val managedFile = storage.resolveManagedBackgroundFile(state.appearance.backgroundImageUri)
        val backgroundHash = if (managedFile != null) {
            state.localBackgroundHash?.takeIf { it.isNotBlank() }
                ?: storage.computeFileSha256(managedFile)
        } else {
            null
        }

        val effectiveRevision = maxOf(state.lastSyncedRevision, state.localModifiedAtMillis)
        val effectiveSyncId = state.lastPublishedSyncId
            .ifBlank { state.lastAppliedSyncId }
            .ifBlank { "state_$effectiveRevision" }

        val payload = SnowAppearanceSyncPayload.fromAppearance(
            sourcePackage = ctx.packageName,
            syncId = effectiveSyncId,
            updatedAtEpochMillis = effectiveRevision,
            appearance = state.appearance.copy(
                backgroundImageUri = if (managedFile != null) state.appearance.backgroundImageUri else null
            ),
            reduceLiveBlur = state.reduceLiveBlur,
            localBackgroundHash = backgroundHash
        )

        return MatrixCursor(arrayOf(PartnerAppConfig.COLUMN_PAYLOAD_JSON), 1).apply {
            addRow(arrayOf(payload.toJson()))
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val ctx = context ?: throw FileNotFoundException("Provider context unavailable")
        val entryPoint = entryPoint(ctx)
        requireTrustedPartnerCaller(ctx, entryPoint)

        if (matchUri(ctx, uri) != CODE_BACKGROUND) {
            throw FileNotFoundException("Unknown URI: $uri")
        }
        if (mode != "r") {
            throw SecurityException("Only read-only mode ('r') is permitted for theme backgrounds")
        }

        val state = runBlocking { entryPoint.settingsDataStore().readSyncState() }
        val file = entryPoint.backgroundImageStorage()
            .resolveManagedBackgroundFile(state.appearance.backgroundImageUri)
            ?: throw FileNotFoundException("No custom background image available")

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? {
        val ctx = context ?: return null
        val entryPoint = entryPoint(ctx)
        requireTrustedPartnerCaller(ctx, entryPoint)
        return when (matchUri(ctx, uri)) {
            CODE_APPEARANCE -> "vnd.android.cursor.item/vnd.${ctx.packageName}.${PartnerAppConfig.PATH_APPEARANCE}"
            CODE_BACKGROUND -> "image/*"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val ctx = context ?: throw SecurityException("Provider context unavailable")
        requireTrustedPartnerCaller(ctx, entryPoint(ctx))
        throw UnsupportedOperationException("ThemeSyncContentProvider is read-only")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val ctx = context ?: throw SecurityException("Provider context unavailable")
        requireTrustedPartnerCaller(ctx, entryPoint(ctx))
        throw UnsupportedOperationException("ThemeSyncContentProvider is read-only")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val ctx = context ?: throw SecurityException("Provider context unavailable")
        requireTrustedPartnerCaller(ctx, entryPoint(ctx))
        throw UnsupportedOperationException("ThemeSyncContentProvider is read-only")
    }

    private fun entryPoint(ctx: Context): SyncProviderEntryPoint =
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            SyncProviderEntryPoint::class.java
        )

    private fun requireTrustedPartnerCaller(
        ctx: Context,
        entryPoint: SyncProviderEntryPoint
    ) {
        val callerPackage = callingPackage
        if (callerPackage != PartnerAppConfig.PARTNER_PACKAGE) {
            throw SecurityException("Untrusted calling package: $callerPackage")
        }
        val uidPackages = ctx.packageManager.getPackagesForUid(Binder.getCallingUid()).orEmpty()
        if (PartnerAppConfig.PARTNER_PACKAGE !in uidPackages) {
            throw SecurityException("Calling UID does not match ${PartnerAppConfig.PARTNER_PACKAGE}")
        }
        if (!entryPoint.partnerAppDetector().isTrustedPartnerPackage(callerPackage)) {
            throw SecurityException("Partner signature verification failed for $callerPackage")
        }
    }

    private fun matchUri(ctx: Context, uri: Uri): Int {
        val authority = "${ctx.packageName}.syncprovider"
        val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(authority, PartnerAppConfig.PATH_APPEARANCE, CODE_APPEARANCE)
            addURI(authority, PartnerAppConfig.PATH_BACKGROUND, CODE_BACKGROUND)
        }
        return matcher.match(uri)
    }

    companion object {
        private const val CODE_APPEARANCE = 1
        private const val CODE_BACKGROUND = 2
    }
}
