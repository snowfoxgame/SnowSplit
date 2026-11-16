package com.snowsplit.app.domain.model

import androidx.compose.runtime.Immutable

enum class EntryKind {
    DEBT,
    PAYMENT
}

enum class DebtDirection {
    THEY_OWE_ME,
    I_OWE_THEM
}

@Immutable
data class DebtEntry(
    val id: Long = 0L,
    val personId: Long,
    val entryKind: EntryKind,
    val direction: DebtDirection,
    val amountCents: Long,
    val note: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val settlementBatchId: String? = null
)
