package com.snowsplit.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_entries",
    foreignKeys = [
        ForeignKey(
            entity = PersonBubbleEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["personId"]),
        Index(value = ["occurredAt"])
    ]
)
data class DebtEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val personId: Long,
    val entryKind: String,
    val direction: String,
    val amountCents: Long,
    val note: String? = null,
    val occurredAt: Long,
    val createdAt: Long,
    val settlementBatchId: String? = null
)
