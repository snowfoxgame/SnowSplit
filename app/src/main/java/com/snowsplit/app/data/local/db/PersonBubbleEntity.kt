package com.snowsplit.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.snowsplit.app.domain.model.AvatarType

@Entity(tableName = "people")
data class PersonBubbleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val avatarType: String = AvatarType.ICON.name,
    val iconKey: String = "person",
    val photoUri: String? = null,
    val anchorAngleRad: Float = 0f,
    val anchorRingFactor: Float = 1f,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
