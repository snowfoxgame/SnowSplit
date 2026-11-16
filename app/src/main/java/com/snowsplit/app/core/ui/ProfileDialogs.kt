package com.snowsplit.app.core.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snowsplit.app.R
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.components.SnowBlurScope
import com.snowsplit.app.core.designsystem.components.SnowFrostedDialog
import com.snowsplit.app.core.designsystem.components.SnowGlassButton
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.AvatarType
import com.snowsplit.app.domain.model.PersonBubble

@Composable
fun MeProfileEditorDialog(
    visible: Boolean,
    settings: AppSettings,
    onImportPhoto: suspend (Uri) -> Result<String>,
    onDismiss: () -> Unit,
    onSave: (displayName: String, avatarType: AvatarType, iconKey: String, photoUri: String?) -> Unit
) {
    val tokens = SnowDosThemeTokens.current
    val defaultMeLabel = stringResource(R.string.bubbles_me_label)

    SnowFrostedDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        maxWidth = 440.dp
    ) {
        var draftName by remember(visible, settings.meDisplayName) {
            mutableStateOf(settings.meDisplayName)
        }
        var draftAvatarType by remember(visible, settings.meAvatarType) {
            mutableStateOf(settings.meAvatarType)
        }
        var draftIconKey by remember(visible, settings.meIconKey) {
            mutableStateOf(settings.meIconKey)
        }
        var draftPhotoUri by remember(visible, settings.mePhotoUri) {
            mutableStateOf(settings.mePhotoUri)
        }
        var avatarError by remember(visible) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_my_profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = draftName,
                onValueChange = { if (it.length <= 24) draftName = it },
                label = { Text(stringResource(R.string.settings_my_profile_name_label)) },
                placeholder = { Text(defaultMeLabel) },
                singleLine = true,
                shape = RoundedCornerShape(tokens.radius.chip),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.settings_my_profile_icon_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AvatarSelectionBlock(
                avatarType = draftAvatarType,
                iconKey = draftIconKey,
                photoUri = draftPhotoUri,
                avatarError = avatarError,
                onImportPhoto = onImportPhoto,
                onAvatarTypeChange = {
                    draftAvatarType = it
                    avatarError = false
                },
                onIconKeyChange = { draftIconKey = it },
                onPhotoImported = {
                    draftPhotoUri = it
                    avatarError = false
                },
                onImportError = { avatarError = true }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm, Alignment.End)
            ) {
                SnowGlassButton(
                    onClick = onDismiss,
                    blurScope = SnowBlurScope.OVERLAY
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                SnowGlassButton(
                    onClick = {
                        onSave(
                            draftName,
                            draftAvatarType,
                            draftIconKey,
                            if (draftAvatarType == AvatarType.PHOTO) draftPhotoUri else null
                        )
                        onDismiss()
                    },
                    highlighted = true,
                    blurScope = SnowBlurScope.OVERLAY
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
fun PersonEditorDialog(
    visible: Boolean,
    editingPerson: PersonBubble?,
    linkedEntryCount: Int,
    onImportPhoto: suspend (Uri) -> Result<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, avatarType: AvatarType, iconKey: String, photoUri: String?) -> Unit,
    onDelete: ((PersonBubble) -> Unit)? = null
) {
    val tokens = SnowDosThemeTokens.current

    SnowFrostedDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        maxWidth = 440.dp
    ) {
        var name by remember(visible, editingPerson?.id) {
            mutableStateOf(editingPerson?.name.orEmpty())
        }
        var avatarType by remember(visible, editingPerson?.id) {
            mutableStateOf(editingPerson?.avatarType ?: AvatarType.ICON)
        }
        var iconKey by remember(visible, editingPerson?.id) {
            mutableStateOf(editingPerson?.iconKey ?: "person")
        }
        var photoUri by remember(visible, editingPerson?.id) {
            mutableStateOf(editingPerson?.photoUri)
        }
        var nameError by remember(visible) { mutableStateOf(false) }
        var avatarError by remember(visible) { mutableStateOf(false) }
        var confirmDelete by remember(visible) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
        ) {
            if (confirmDelete && editingPerson != null && onDelete != null) {
                Text(
                    text = stringResource(R.string.person_dialog_delete_confirm_title, editingPerson.name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (linkedEntryCount > 0) {
                        stringResource(
                            R.string.person_dialog_delete_confirm_body,
                            editingPerson.name,
                            linkedEntryCount
                        )
                    } else {
                        stringResource(
                            R.string.person_dialog_delete_confirm_empty_body,
                            editingPerson.name
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm, Alignment.End)
                ) {
                    SnowGlassButton(
                        onClick = { confirmDelete = false },
                        blurScope = SnowBlurScope.OVERLAY
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    SnowGlassButton(
                        onClick = {
                            onDelete(editingPerson)
                            onDismiss()
                        },
                        highlighted = true,
                        blurScope = SnowBlurScope.OVERLAY
                    ) {
                        Text(
                            text = stringResource(R.string.person_dialog_delete_cta),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    text = if (editingPerson == null) {
                        stringResource(R.string.person_dialog_new_title)
                    } else {
                        stringResource(R.string.person_dialog_edit_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 28) {
                            name = it
                            nameError = false
                        }
                    },
                    label = { Text(stringResource(R.string.person_dialog_name_label)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.person_dialog_name_error)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(tokens.radius.chip),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.person_dialog_avatar_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AvatarSelectionBlock(
                    avatarType = avatarType,
                    iconKey = iconKey,
                    photoUri = photoUri,
                    avatarError = avatarError,
                    onImportPhoto = onImportPhoto,
                    onAvatarTypeChange = {
                        avatarType = it
                        avatarError = false
                    },
                    onIconKeyChange = { iconKey = it },
                    onPhotoImported = {
                        photoUri = it
                        avatarError = false
                    },
                    onImportError = { avatarError = true }
                )

                if (editingPerson != null && onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = stringResource(R.string.person_dialog_delete_cta),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm, Alignment.End)
                ) {
                    SnowGlassButton(
                        onClick = onDismiss,
                        blurScope = SnowBlurScope.OVERLAY
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    SnowGlassButton(
                        onClick = {
                            val trimmed = name.trim()
                            if (trimmed.isEmpty()) {
                                nameError = true
                            } else {
                                onSave(
                                    trimmed,
                                    avatarType,
                                    iconKey,
                                    if (avatarType == AvatarType.PHOTO) photoUri else null
                                )
                                onDismiss()
                            }
                        },
                        highlighted = true,
                        blurScope = SnowBlurScope.OVERLAY
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}
