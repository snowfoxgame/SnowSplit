package com.snowsplit.app.domain.usecase

import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.DebtDirection
import com.snowsplit.app.domain.model.DebtEntry
import com.snowsplit.app.domain.model.EntryKind
import com.snowsplit.app.domain.model.PersonBubble
import com.snowsplit.app.domain.model.SettleTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtCalculationEngineTest {

    private val engine = DebtCalculationEngine()
    private val max = PersonBubble(id = 1L, name = "Max")
    private val lina = PersonBubble(id = 2L, name = "Lina")

    @Test
    fun `gross mode keeps credits and debts independent while net mode offsets them`() {
        val entries = listOf(
            DebtEntry(id = 1, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.THEY_OWE_ME, amountCents = 2000L, occurredAt = 100L),
            DebtEntry(id = 2, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.I_OWE_THEM, amountCents = 6000L, occurredAt = 200L)
        )

        val gross = engine.projectPersonSummary(max, entries, DebtCalculationMode.GROSS)
        assertEquals(2000L, gross.displayGreenCents)
        assertEquals(6000L, gross.displayRedCents)
        assertEquals(-4000L, gross.netCents)

        val net = engine.projectPersonSummary(max, entries, DebtCalculationMode.NET)
        assertEquals(0L, net.displayGreenCents)
        assertEquals(4000L, net.displayRedCents)
        assertEquals(-4000L, net.netCents)
    }

    @Test
    fun `partial payment reduces matching bucket without affecting opposite bucket`() {
        val entries = listOf(
            DebtEntry(id = 1, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.THEY_OWE_ME, amountCents = 2000L, occurredAt = 100L),
            DebtEntry(id = 2, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.I_OWE_THEM, amountCents = 6000L, occurredAt = 200L),
            DebtEntry(id = 3, personId = 1L, entryKind = EntryKind.PAYMENT, direction = DebtDirection.I_OWE_THEM, amountCents = 1500L, occurredAt = 300L)
        )

        val gross = engine.projectPersonSummary(max, entries, DebtCalculationMode.GROSS)
        assertEquals(2000L, gross.displayGreenCents)
        assertEquals(4500L, gross.displayRedCents)

        val net = engine.projectPersonSummary(max, entries, DebtCalculationMode.NET)
        assertEquals(0L, net.displayGreenCents)
        assertEquals(2500L, net.displayRedCents)
    }

    @Test
    fun `overpayment spills excess into opposite bucket cleanly`() {
        val entries = listOf(
            DebtEntry(id = 1, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.THEY_OWE_ME, amountCents = 2000L, occurredAt = 100L),
            DebtEntry(id = 2, personId = 1L, entryKind = EntryKind.PAYMENT, direction = DebtDirection.THEY_OWE_ME, amountCents = 5000L, occurredAt = 200L)
        )

        val gross = engine.projectPersonSummary(max, entries, DebtCalculationMode.GROSS)
        assertEquals(0L, gross.displayGreenCents)
        assertEquals(3000L, gross.displayRedCents)
        assertEquals(-3000L, gross.netCents)
    }

    @Test
    fun `overpayment in reverse direction plus subsequent debt tracks both buckets accurately`() {
        val entries = listOf(
            DebtEntry(id = 1, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.I_OWE_THEM, amountCents = 1000L, occurredAt = 100L),
            DebtEntry(id = 2, personId = 1L, entryKind = EntryKind.PAYMENT, direction = DebtDirection.I_OWE_THEM, amountCents = 3500L, occurredAt = 200L),
            DebtEntry(id = 3, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.I_OWE_THEM, amountCents = 500L, occurredAt = 300L)
        )

        val gross = engine.projectPersonSummary(max, entries, DebtCalculationMode.GROSS)
        assertEquals(2500L, gross.displayGreenCents)
        assertEquals(500L, gross.displayRedCents)

        val net = engine.projectPersonSummary(max, entries, DebtCalculationMode.NET)
        assertEquals(2000L, net.displayGreenCents)
        assertEquals(0L, net.displayRedCents)
    }

    @Test
    fun `settle ALL zeroes both buckets in both GROSS and NET modes`() {
        val existing = listOf(
            DebtEntry(id = 1, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.THEY_OWE_ME, amountCents = 2000L, occurredAt = 100L),
            DebtEntry(id = 2, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.I_OWE_THEM, amountCents = 6000L, occurredAt = 200L)
        )

        val settlements = engine.buildSettlementEntries(
            personId = 1L,
            existingEntries = existing,
            target = SettleTarget.ALL,
            nowMillis = 500L
        )
        assertEquals(2, settlements.size)
        assertTrue(settlements.all { it.settlementBatchId != null })

        val updated = existing + settlements
        val grossAfter = engine.projectPersonSummary(max, updated, DebtCalculationMode.GROSS)
        val netAfter = engine.projectPersonSummary(max, updated, DebtCalculationMode.NET)
        assertEquals(0L, grossAfter.displayGreenCents)
        assertEquals(0L, grossAfter.displayRedCents)
        assertEquals(0L, netAfter.displayGreenCents)
        assertEquals(0L, netAfter.displayRedCents)
    }

    @Test
    fun `settle single bucket leaves opposite bucket untouched`() {
        val existing = listOf(
            DebtEntry(id = 1, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.THEY_OWE_ME, amountCents = 2000L, occurredAt = 100L),
            DebtEntry(id = 2, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.I_OWE_THEM, amountCents = 6000L, occurredAt = 200L)
        )

        val settleGreen = engine.buildSettlementEntries(
            personId = 1L,
            existingEntries = existing,
            target = SettleTarget.THEY_OWE_ME_ONLY,
            nowMillis = 500L
        )
        assertEquals(1, settleGreen.size)

        val grossAfter = engine.projectPersonSummary(max, existing + settleGreen, DebtCalculationMode.GROSS)
        assertEquals(0L, grossAfter.displayGreenCents)
        assertEquals(6000L, grossAfter.displayRedCents)
    }

    @Test
    fun `total overview aggregates across multiple people`() {
        val entries = listOf(
            DebtEntry(id = 1, personId = 1L, entryKind = EntryKind.DEBT, direction = DebtDirection.THEY_OWE_ME, amountCents = 2500L, occurredAt = 100L),
            DebtEntry(id = 2, personId = 2L, entryKind = EntryKind.DEBT, direction = DebtDirection.I_OWE_THEM, amountCents = 1000L, occurredAt = 150L)
        )
        val summaries = engine.calculateAllSummaries(listOf(max, lina), entries, DebtCalculationMode.NET)
        val overview = engine.calculateTotalOverview(summaries)
        assertEquals(2500L, overview.totalGreenCents)
        assertEquals(1000L, overview.totalRedCents)
        assertEquals(1500L, overview.netCents)
    }
}
