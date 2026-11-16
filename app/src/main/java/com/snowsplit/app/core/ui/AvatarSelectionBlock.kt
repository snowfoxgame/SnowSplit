package com.snowsplit.app.core.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.snowsplit.app.R
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.components.SnowBlurScope
import com.snowsplit.app.core.designsystem.components.SnowGlassButton
import com.snowsplit.app.core.designsystem.components.rememberDecodedBackgroundBitmap
import com.snowsplit.app.domain.model.AvatarType
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarSelectionBlock(
    avatarType: AvatarType,
    iconKey: String,
    photoUri: String?,
    avatarError: Boolean,
    onImportPhoto: suspend (Uri) -> Result<String>,
    onAvatarTypeChange: (AvatarType) -> Unit,
    onIconKeyChange: (String) -> Unit,
    onPhotoImported: (String) -> Unit,
    onImportError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SnowDosThemeTokens.current
    val coroutineScope = rememberCoroutineScope()
    val itemShape = if (tokens.surface.usePixelRetroStyle) {
        RoundedCornerShape(tokens.radius.chip)
    } else {
        CircleShape
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                onImportPhoto(uri)
                    .onSuccess { localUri ->
                        onPhotoImported(localUri)
                        onAvatarTypeChange(AvatarType.PHOTO)
                    }
                    .onFailure {
                        onImportError()
                    }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SnowGlassButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                blurScope = SnowBlurScope.OVERLAY,
                highlighted = avatarType == AvatarType.PHOTO
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.entry_dialog_pick_photo))
            }

            if (avatarType == AvatarType.PHOTO && !photoUri.isNullOrBlank()) {
                val bmp = rememberDecodedBackgroundBitmap(
                    uriString = photoUri,
                    maxWidthPx = 128,
                    maxHeightPx = 128
                )
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(itemShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary, itemShape)
                    )
                }
                TextButton(onClick = { onAvatarTypeChange(AvatarType.ICON) }) {
                    Text(stringResource(R.string.entry_dialog_use_icon))
                }
            }
        }

        if (avatarError) {
            Text(
                text = stringResource(R.string.entry_dialog_avatar_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (avatarType == AvatarType.ICON) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BubbleIconCatalog.options.forEach { option ->
                    val isSelected = iconKey == option.key
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(itemShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                shape = itemShape
                            )
                            .clickable { onIconKeyChange(option.key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
