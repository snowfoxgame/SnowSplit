package com.snowsplit.app.data.local.db

import com.snowsplit.app.domain.model.AvatarType
import com.snowsplit.app.domain.model.DebtDirection
import com.snowsplit.app.domain.model.DebtEntry
import com.snowsplit.app.domain.model.EntryKind
import com.snowsplit.app.domain.model.PersonBubble

fun PersonBubbleEntity.toDomain(): PersonBubble = PersonBubble(
    id = id,
    name = name,
    avatarType = runCatching { AvatarType.valueOf(avatarType) }.getOrDefault(AvatarType.ICON),
    iconKey = iconKey,
    photoUri = photoUri,
    anchorAngleRad = anchorAngleRad,
    anchorRingFactor = anchorRingFactor,
    isArchived = isArchived,
    createdAt = createdAt
)

fun PersonBubble.toEntity(): PersonBubbleEntity = PersonBubbleEntity(
    id = id,
    name = name.trim(),
    avatarType = avatarType.name,
    iconKey = iconKey,
    photoUri = photoUri,
    anchorAngleRad = anchorAngleRad,
    anchorRingFactor = anchorRingFactor,
    isArchived = isArchived,
    createdAt = createdAt
)

fun DebtEntryEntity.toDomain(): DebtEntry = DebtEntry(
    id = id,
    personId = personId,
    entryKind = runCatching { EntryKind.valueOf(entryKind) }.getOrDefault(EntryKind.DEBT),
    direction = runCatching { DebtDirection.valueOf(direction) }.getOrDefault(DebtDirection.THEY_OWE_ME),
    amountCents = amountCents,
    note = note,
    occurredAt = occurredAt,
    createdAt = createdAt,
    settlementBatchId = settlementBatchId
)

fun DebtEntry.toEntity(): DebtEntryEntity = DebtEntryEntity(
    id = id,
    personId = personId,
    entryKind = entryKind.name,
    direction = direction.name,
    amountCents = amountCents,
    note = note?.trim()?.takeIf { it.isNotEmpty() },
    occurredAt = occurredAt,
    createdAt = createdAt,
    settlementBatchId = settlementBatchId
)
