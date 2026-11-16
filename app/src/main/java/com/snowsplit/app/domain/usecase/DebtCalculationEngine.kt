package com.snowsplit.app.domain.usecase

import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.DebtDirection
import com.snowsplit.app.domain.model.DebtEntry
import com.snowsplit.app.domain.model.EntryKind
import com.snowsplit.app.domain.model.PersonBalanceSummary
import com.snowsplit.app.domain.model.PersonBubble
import com.snowsplit.app.domain.model.RawPersonBuckets
import com.snowsplit.app.domain.model.SettleTarget
import com.snowsplit.app.domain.model.TotalBalanceOverview
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure deterministic ledger engine computing per-person and total balances in exact minor units (cents).
 * Supports both GROSS (separate credit/debt buckets) and NET (automatic offset) projections,
 * chronological partial payments, overpayment spillover into the opposite bucket, and full settlement.
 */
@Singleton
class DebtCalculationEngine @Inject constructor() {

    fun calculateRawBuckets(entries: List<DebtEntry>): RawPersonBuckets {
        if (entries.isEmpty()) return RawPersonBuckets()

        var theyOweMe = 0L
        var iOweThem = 0L

        val ordered = entries.sortedWith(
            compareBy<DebtEntry> { it.occurredAt }
                .thenBy { it.createdAt }
                .thenBy { it.id }
        )

        for (entry in ordered) {
            val amount = entry.amountCents
            if (amount <= 0L) continue

            when (entry.entryKind) {
                EntryKind.DEBT -> when (entry.direction) {
                    DebtDirection.THEY_OWE_ME -> theyOweMe += amount
                    DebtDirection.I_OWE_THEM -> iOweThem += amount
                }

                EntryKind.PAYMENT -> when (entry.direction) {
                    DebtDirection.THEY_OWE_ME -> {
                        if (amount <= theyOweMe) {
                            theyOweMe -= amount
                        } else {
                            val spillover = amount - theyOweMe
                            theyOweMe = 0L
                            iOweThem += spillover
                        }
                    }

                    DebtDirection.I_OWE_THEM -> {
                        if (amount <= iOweThem) {
                            iOweThem -= amount
                        } else {
                            val spillover = amount - iOweThem
                            iOweThem = 0L
                            theyOweMe += spillover
                        }
                    }
                }
            }
        }

        return RawPersonBuckets(
            theyOweMeCents = theyOweMe,
            iOweThemCents = iOweThem
        )
    }

    fun projectPersonSummary(
        person: PersonBubble,
        entries: List<DebtEntry>,
        mode: DebtCalculationMode
    ): PersonBalanceSummary {
        val raw = calculateRawBuckets(entries)
        val net = raw.theyOweMeCents - raw.iOweThemCents

        val (displayGreen, displayRed) = when (mode) {
            DebtCalculationMode.GROSS -> raw.theyOweMeCents to raw.iOweThemCents
            DebtCalculationMode.NET -> when {
                net > 0L -> net to 0L
                net < 0L -> 0L to -net
                else -> 0L to 0L
            }
        }

        return PersonBalanceSummary(
            person = person,
            rawTheyOweMeCents = raw.theyOweMeCents,
            rawIOweThemCents = raw.iOweThemCents,
            displayGreenCents = displayGreen,
            displayRedCents = displayRed,
            netCents = net,
            entryCount = entries.size
        )
    }

    fun calculateAllSummaries(
        people: List<PersonBubble>,
        entries: List<DebtEntry>,
        mode: DebtCalculationMode
    ): List<PersonBalanceSummary> {
        val grouped = entries.groupBy { it.personId }
        return people.map { person ->
            projectPersonSummary(
                person = person,
                entries = grouped[person.id].orEmpty(),
                mode = mode
            )
        }
    }

    fun calculateTotalOverview(summaries: List<PersonBalanceSummary>): TotalBalanceOverview {
        var totalGreen = 0L
        var totalRed = 0L
        for (summary in summaries) {
            totalGreen += summary.displayGreenCents
            totalRed += summary.displayRedCents
        }
        return TotalBalanceOverview(
            totalGreenCents = totalGreen,
            totalRedCents = totalRed,
            netCents = totalGreen - totalRed
        )
    }

    fun buildSettlementEntries(
        personId: Long,
        existingEntries: List<DebtEntry>,
        target: SettleTarget,
        note: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DebtEntry> {
        val raw = calculateRawBuckets(existingEntries)
        val batchId = UUID.randomUUID().toString()
        val result = mutableListOf<DebtEntry>()

        when (target) {
            SettleTarget.ALL -> {
                if (raw.theyOweMeCents > 0L) {
                    result += DebtEntry(
                        personId = personId,
                        entryKind = EntryKind.PAYMENT,
                        direction = DebtDirection.THEY_OWE_ME,
                        amountCents = raw.theyOweMeCents,
                        note = note,
                        occurredAt = nowMillis,
                        createdAt = nowMillis,
                        settlementBatchId = if (raw.iOweThemCents > 0L) batchId else null
                    )
                }
                if (raw.iOweThemCents > 0L) {
                    result += DebtEntry(
                        personId = personId,
                        entryKind = EntryKind.PAYMENT,
                        direction = DebtDirection.I_OWE_THEM,
                        amountCents = raw.iOweThemCents,
                        note = note,
                        occurredAt = nowMillis,
                        createdAt = nowMillis + 1L,
                        settlementBatchId = if (raw.theyOweMeCents > 0L) batchId else null
                    )
                }
            }

            SettleTarget.THEY_OWE_ME_ONLY -> {
                if (raw.theyOweMeCents > 0L) {
                    result += DebtEntry(
                        personId = personId,
                        entryKind = EntryKind.PAYMENT,
                        direction = DebtDirection.THEY_OWE_ME,
                        amountCents = raw.theyOweMeCents,
                        note = note,
                        occurredAt = nowMillis,
                        createdAt = nowMillis
                    )
                }
            }

            SettleTarget.I_OWE_THEM_ONLY -> {
                if (raw.iOweThemCents > 0L) {
                    result += DebtEntry(
                        personId = personId,
                        entryKind = EntryKind.PAYMENT,
                        direction = DebtDirection.I_OWE_THEM,
                        amountCents = raw.iOweThemCents,
                        note = note,
                        occurredAt = nowMillis,
                        createdAt = nowMillis
                    )
                }
            }
        }

        return result
    }
}
