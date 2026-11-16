package com.snowsplit.app.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.components.rememberDecodedBackgroundBitmap
import com.snowsplit.app.domain.model.AvatarType
import com.snowsplit.app.domain.model.PersonBubble

@Composable
fun PersonAvatarBadge(
    person: PersonBubble,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    AvatarBadge(
        avatarType = person.avatarType,
        iconKey = person.iconKey,
        photoUri = person.photoUri,
        contentDescription = person.name,
        size = size,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun AvatarBadge(
    avatarType: AvatarType,
    iconKey: String,
    photoUri: String?,
    contentDescription: String?,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val tokens = SnowDosThemeTokens.current
    val shape = if (tokens.surface.usePixelRetroStyle) {
        RoundedCornerShape(tokens.radius.chip)
    } else {
        CircleShape
    }
    val decodePx = with(LocalDensity.current) { (size * 2).roundToPx().coerceIn(96, 512) }
    val photoBitmap = if (avatarType == AvatarType.PHOTO && !photoUri.isNullOrBlank()) {
        rememberDecodedBackgroundBitmap(
            uriString = photoUri,
            maxWidthPx = decodePx,
            maxHeightPx = decodePx
        )
    } else {
        null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (photoBitmap != null) {
            Image(
                bitmap = photoBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = BubbleIconCatalog.resolve(iconKey),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }
}
