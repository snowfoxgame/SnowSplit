package com.snowsplit.app.domain.model

import androidx.compose.runtime.Immutable

enum class AvatarType {
    ICON,
    PHOTO
}

@Immutable
data class PersonBubble(
    val id: Long = 0L,
    val name: String,
    val avatarType: AvatarType = AvatarType.ICON,
    val iconKey: String = "person",
    val photoUri: String? = null,
    val anchorAngleRad: Float = 0f,
    val anchorRingFactor: Float = 1f,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
