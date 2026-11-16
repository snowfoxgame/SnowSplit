package com.snowsplit.app.platform.sync

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.snowsplit.app.core.common.DispatcherProvider
import com.snowsplit.app.data.local.prefs.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class PackageSignatureSnapshot(
    val packageName: String,
    val isEnabled: Boolean,
    val sha256Digests: Set<String>
)

fun interface PartnerPackageInspector {
    fun inspect(packageName: String): PackageSignatureSnapshot?
}

object PartnerSignatureVerifier {
    fun isPartnerValid(
        ownSnapshot: PackageSignatureSnapshot?,
        partnerSnapshot: PackageSignatureSnapshot?,
        expectedPartnerPackage: String = PartnerAppConfig.PARTNER_PACKAGE
    ): Boolean {
        if (ownSnapshot == null || partnerSnapshot == null) return false
        if (partnerSnapshot.packageName != expectedPartnerPackage) return false
        if (!partnerSnapshot.isEnabled) return false
        if (ownSnapshot.sha256Digests.isEmpty() || partnerSnapshot.sha256Digests.isEmpty()) {
            return false
        }
        return ownSnapshot.sha256Digests == partnerSnapshot.sha256Digests
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

class AndroidPartnerPackageInspector(
    private val context: Context
) : PartnerPackageInspector {

    override fun inspect(packageName: String): PackageSignatureSnapshot? {
        val packageManager = context.packageManager
        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
        }.getOrNull() ?: return null

        val isEnabled = packageInfo.applicationInfo?.enabled == true
        val digests = extractSha256Digests(packageInfo)
        return PackageSignatureSnapshot(
            packageName = packageName,
            isEnabled = isEnabled,
            sha256Digests = digests
        )
    }

    private fun extractSha256Digests(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
                    ?.takeIf { it.isNotEmpty() }
                    ?: signingInfo.apkContentsSigners
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        } ?: return emptySet()

        return signatures
            .map { PartnerSignatureVerifier.sha256Hex(it.toByteArray()) }
            .toSet()
    }
}

class PartnerConnectionTracker(
    private val ownPackageName: String,
    private val inspector: PartnerPackageInspector,
    private val persistConnectedState: suspend (Boolean) -> Unit
) {
    private val _partnerAppConnected = MutableStateFlow(false)
    val partnerAppConnected: StateFlow<Boolean> = _partnerAppConnected.asStateFlow()

    suspend fun refreshConnectionStatus(): Boolean {
        val connected = isPartnerTrustedNow()
        if (_partnerAppConnected.value != connected) {
            _partnerAppConnected.value = connected
        }
        persistConnectedState(connected)
        return connected
    }

    fun isPartnerTrustedNow(candidatePackage: String = PartnerAppConfig.PARTNER_PACKAGE): Boolean {
        if (candidatePackage != PartnerAppConfig.PARTNER_PACKAGE) return false
        val own = inspector.inspect(ownPackageName)
        val partner = inspector.inspect(PartnerAppConfig.PARTNER_PACKAGE)
        return PartnerSignatureVerifier.isPartnerValid(
            ownSnapshot = own,
            partnerSnapshot = partner,
            expectedPartnerPackage = PartnerAppConfig.PARTNER_PACKAGE
        )
    }
}

@Singleton
class PartnerAppDetector @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val dispatcherProvider: DispatcherProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    private val tracker = PartnerConnectionTracker(
        ownPackageName = context.packageName,
        inspector = AndroidPartnerPackageInspector(context),
        persistConnectedState = { connected ->
            settingsDataStore.setPartnerAppConnected(connected)
        }
    )

    val partnerAppConnected: StateFlow<Boolean> = tracker.partnerAppConnected

    init {
        scope.launch {
            tracker.refreshConnectionStatus()
        }
    }

    suspend fun refreshConnectionStatus(): Boolean = withContext(dispatcherProvider.io) {
        tracker.refreshConnectionStatus()
    }

    fun isTrustedPartnerPackage(packageName: String?): Boolean =
        packageName != null && tracker.isPartnerTrustedNow(packageName)

    fun refreshAsync() {
        scope.launch {
            tracker.refreshConnectionStatus()
        }
    }
}
