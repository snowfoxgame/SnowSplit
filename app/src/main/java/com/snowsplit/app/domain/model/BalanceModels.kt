package com.snowsplit.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class RawPersonBuckets(
    val theyOweMeCents: Long = 0L,
    val iOweThemCents: Long = 0L
)

@Immutable
data class PersonBalanceSummary(
    val person: PersonBubble,
    val rawTheyOweMeCents: Long,
    val rawIOweThemCents: Long,
    val displayGreenCents: Long,
    val displayRedCents: Long,
    val netCents: Long,
    val entryCount: Int
)

@Immutable
data class TotalBalanceOverview(
    val totalGreenCents: Long = 0L,
    val totalRedCents: Long = 0L,
    val netCents: Long = 0L
)

enum class SettleTarget {
    ALL,
    THEY_OWE_ME_ONLY,
    I_OWE_THEM_ONLY
}
