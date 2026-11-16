package com.snowsplit.app.platform.sync

import com.snowsplit.app.domain.model.AppSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartnerAppDetectorTest {

    @Test
    fun `signature verifier validates matching digests only when partner is installed and enabled`() {
        val own = PackageSignatureSnapshot(
            packageName = PartnerAppConfig.OWN_PACKAGE,
            isEnabled = true,
            sha256Digests = setOf("deadbeef0123")
        )
        val matchingPartner = PackageSignatureSnapshot(
            packageName = PartnerAppConfig.PARTNER_PACKAGE,
            isEnabled = true,
            sha256Digests = setOf("deadbeef0123")
        )
        val disabledPartner = matchingPartner.copy(isEnabled = false)
        val wrongSigPartner = matchingPartner.copy(sha256Digests = setOf("cafebabe9999"))
        val wrongPkg = matchingPartner.copy(packageName = "com.other.app")

        assertTrue(PartnerSignatureVerifier.isPartnerValid(own, matchingPartner))
        assertFalse(PartnerSignatureVerifier.isPartnerValid(own, null))
        assertFalse(PartnerSignatureVerifier.isPartnerValid(own, disabledPartner))
        assertFalse(PartnerSignatureVerifier.isPartnerValid(own, wrongSigPartner))
        assertFalse(PartnerSignatureVerifier.isPartnerValid(own, wrongPkg))
    }

    @Test
    fun `connection tracker reactively transitions false to true and back to false on uninstall`() =
        runTest {
            var partnerInstalled = false
            var partnerEnabled = true
            var partnerSig = "matching_sha256"
            val persistedStates = mutableListOf<Boolean>()

            val inspector = PartnerPackageInspector { pkg ->
                when (pkg) {
                    PartnerAppConfig.OWN_PACKAGE -> PackageSignatureSnapshot(
                        packageName = PartnerAppConfig.OWN_PACKAGE,
                        isEnabled = true,
                        sha256Digests = setOf("matching_sha256")
                    )
                    PartnerAppConfig.PARTNER_PACKAGE -> if (!partnerInstalled) {
                        null
                    } else {
                        PackageSignatureSnapshot(
                            packageName = PartnerAppConfig.PARTNER_PACKAGE,
                            isEnabled = partnerEnabled,
                            sha256Digests = setOf(partnerSig)
                        )
                    }
                    else -> null
                }
            }

            val tracker = PartnerConnectionTracker(
                ownPackageName = PartnerAppConfig.OWN_PACKAGE,
                inspector = inspector,
                persistConnectedState = { persistedStates.add(it) }
            )

            // 1. Initially uninstalled
            assertFalse(tracker.refreshConnectionStatus())
            assertFalse(tracker.partnerAppConnected.value)

            // 2. SnowDo's installed with matching signature
            partnerInstalled = true
            assertTrue(tracker.refreshConnectionStatus())
            assertTrue(tracker.partnerAppConnected.value)

            // 3. SnowDo's disabled -> drops to false
            partnerEnabled = false
            assertFalse(tracker.refreshConnectionStatus())
            assertFalse(tracker.partnerAppConnected.value)

            // 4. Re-enabled -> back to true
            partnerEnabled = true
            assertTrue(tracker.refreshConnectionStatus())
            assertTrue(tracker.partnerAppConnected.value)

            // 5. Uninstalled -> automatically flips back to false
            partnerInstalled = false
            assertFalse(tracker.refreshConnectionStatus())
            assertFalse(tracker.partnerAppConnected.value)

            // 6. Reinstalled with mismatching signature -> stays false
            partnerInstalled = true
            partnerSig = "different_sha256"
            assertFalse(tracker.refreshConnectionStatus())
            assertFalse(tracker.partnerAppConnected.value)

            assertEquals(listOf(false, true, false, true, false, false), persistedStates)
        }

    @Test
    fun `default AppSettings enables cross-app sync out of the box`() {
        val defaults = AppSettings()
        assertTrue(defaults.crossAppSyncEnabled)
        assertFalse(defaults.partnerAppConnected)
    }
}
