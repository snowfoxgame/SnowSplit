package com.snowsplit.app.feature.entries

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.snowsplit.app.R
import com.snowsplit.app.core.common.AmountParser
import com.snowsplit.app.core.common.CurrencyFormatter
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.components.SnowBlurScope
import com.snowsplit.app.core.designsystem.components.SnowFrostedDialog
import com.snowsplit.app.core.designsystem.components.SnowGlassButton
import com.snowsplit.app.core.designsystem.components.SnowGlassChip
import com.snowsplit.app.core.designsystem.components.SnowSegmentOption
import com.snowsplit.app.core.designsystem.components.SnowSegmentedPill
import com.snowsplit.app.core.ui.AvatarSelectionBlock
import com.snowsplit.app.domain.model.AvatarType
import com.snowsplit.app.domain.model.CurrencyOption
import com.snowsplit.app.domain.model.DebtDirection
import com.snowsplit.app.domain.model.DebtEntry
import com.snowsplit.app.domain.model.EntryKind
import com.snowsplit.app.domain.model.PersonBubble
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorDialog(
    visible: Boolean,
    editingEntry: DebtEntry?,
    people: List<PersonBubble>,
    currencyOption: CurrencyOption,
    onImportAvatarPhoto: suspend (Uri) -> Result<String>,
    onDismiss: () -> Unit,
    onSave: (
        existingEntryId: Long?,
        existingPersonId: Long?,
        newPersonName: String,
        newPersonAvatarType: AvatarType,
        newPersonIconKey: String,
        newPersonPhotoUri: String?,
        entryKind: EntryKind,
        direction: DebtDirection,
        amountCents: Long,
        note: String?,
        occurredAt: Long
    ) -> Unit
) {
    val tokens = SnowDosThemeTokens.current

    var selectedPersonId by remember(visible, editingEntry, people) {
        mutableStateOf(editingEntry?.personId ?: people.firstOrNull()?.id)
    }
    var creatingNewPerson by remember(visible, editingEntry, people) {
        mutableStateOf(editingEntry == null && people.isEmpty())
    }
    var newPersonName by remember(visible, editingEntry) { mutableStateOf("") }
    var newPersonAvatarType by remember(visible, editingEntry) { mutableStateOf(AvatarType.ICON) }
    var newPersonIconKey by remember(visible, editingEntry) { mutableStateOf("person") }
    var newPersonPhotoUri by remember(visible, editingEntry) { mutableStateOf<String?>(null) }
    var avatarError by remember(visible) { mutableStateOf(false) }

    var entryKind by remember(visible, editingEntry) {
        mutableStateOf(editingEntry?.entryKind ?: EntryKind.DEBT)
    }
    var direction by remember(visible, editingEntry) {
        mutableStateOf(editingEntry?.direction ?: DebtDirection.THEY_OWE_ME)
    }
    var amountText by remember(visible, editingEntry) {
        mutableStateOf(editingEntry?.let { AmountParser.formatInputFromCents(it.amountCents) } ?: "")
    }
    var noteText by remember(visible, editingEntry) {
        mutableStateOf(editingEntry?.note ?: "")
    }
    var occurredAt by remember(visible, editingEntry) {
        mutableLongStateOf(editingEntry?.occurredAt ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var personError by remember(visible) { mutableStateOf(false) }
    var amountError by remember(visible) { mutableStateOf(false) }

    val effectiveCurrencySymbol = remember(currencyOption) {
        CurrencyFormatter.resolveEffectiveCurrency(currencyOption).symbol ?: "€"
    }
    val selectedPersonName = if (creatingNewPerson) {
        newPersonName.trim().ifBlank { stringResource(R.string.entry_dialog_person_fallback) }
    } else {
        people.firstOrNull { it.id == selectedPersonId }?.name
            ?: stringResource(R.string.entry_dialog_person_fallback)
    }

    SnowFrostedDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        maxWidth = 460.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
        ) {
            Text(
                text = if (editingEntry == null) {
                    stringResource(R.string.entry_dialog_new_title)
                } else {
                    stringResource(R.string.entry_dialog_edit_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            // Person selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.entry_dialog_person_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(people, key = { it.id }) { person ->
                        SnowGlassChip(
                            label = person.name,
                            selected = !creatingNewPerson && selectedPersonId == person.id,
                            blurScope = SnowBlurScope.OVERLAY,
                            onClick = {
                                creatingNewPerson = false
                                selectedPersonId = person.id
                                personError = false
                            }
                        )
                    }
                    if (editingEntry == null) {
                        item(key = "new_person_chip") {
                            SnowGlassChip(
                                label = stringResource(R.string.entry_dialog_new_person_chip),
                                selected = creatingNewPerson,
                                blurScope = SnowBlurScope.OVERLAY,
                                onClick = {
                                    creatingNewPerson = true
                                    personError = false
                                }
                            )
                        }
                    }
                }
            }

            if (creatingNewPerson) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPersonName,
                        onValueChange = {
                            if (it.length <= 28) {
                                newPersonName = it
                                personError = false
                            }
                        },
                        label = { Text(stringResource(R.string.entry_dialog_new_person_name)) },
                        isError = personError,
                        singleLine = true,
                        shape = RoundedCornerShape(tokens.radius.chip),
                        modifier = Modifier.fillMaxWidth()
                    )

                    AvatarSelectionBlock(
                        avatarType = newPersonAvatarType,
                        iconKey = newPersonIconKey,
                        photoUri = newPersonPhotoUri,
                        avatarError = avatarError,
                        onImportPhoto = onImportAvatarPhoto,
                        onAvatarTypeChange = {
                            newPersonAvatarType = it
                            avatarError = false
                        },
                        onIconKeyChange = { newPersonIconKey = it },
                        onPhotoImported = {
                            newPersonPhotoUri = it
                            avatarError = false
                        },
                        onImportError = { avatarError = true }
                    )
                }
            }

            if (personError && !creatingNewPerson) {
                Text(
                    text = stringResource(R.string.entry_dialog_person_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Kind (New debt vs Repayment)
            SnowSegmentedPill(
                options = listOf(
                    SnowSegmentOption(EntryKind.DEBT, stringResource(R.string.entry_dialog_kind_debt)),
                    SnowSegmentOption(EntryKind.PAYMENT, stringResource(R.string.entry_dialog_kind_payment))
                ),
                selected = entryKind,
                onSelect = { entryKind = it },
                blurScope = SnowBlurScope.OVERLAY
            )

            // Direction
            val dirLeftLabel = if (entryKind == EntryKind.DEBT) {
                stringResource(R.string.entry_dialog_debt_they_owe_me, selectedPersonName)
            } else {
                stringResource(R.string.entry_dialog_pay_they_paid_me, selectedPersonName)
            }
            val dirRightLabel = if (entryKind == EntryKind.DEBT) {
                stringResource(R.string.entry_dialog_debt_i_owe_them, selectedPersonName)
            } else {
                stringResource(R.string.entry_dialog_pay_i_paid_them, selectedPersonName)
            }
            SnowSegmentedPill(
                options = listOf(
                    SnowSegmentOption(DebtDirection.THEY_OWE_ME, dirLeftLabel),
                    SnowSegmentOption(DebtDirection.I_OWE_THEM, dirRightLabel)
                ),
                selected = direction,
                onSelect = { direction = it },
                blurScope = SnowBlurScope.OVERLAY
            )

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { raw ->
                    if (raw.length <= 12) {
                        amountText = raw
                        amountError = false
                    }
                },
                label = { Text(stringResource(R.string.entry_dialog_amount_label, effectiveCurrencySymbol)) },
                isError = amountError,
                supportingText = if (amountError) {
                    { Text(stringResource(R.string.entry_dialog_amount_error)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(tokens.radius.chip),
                modifier = Modifier.fillMaxWidth()
            )

            // Note
            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 80) noteText = it },
                label = { Text(stringResource(R.string.entry_dialog_note_label)) },
                singleLine = true,
                shape = RoundedCornerShape(tokens.radius.chip),
                modifier = Modifier.fillMaxWidth()
            )

            // Date
            val formattedDate = remember(occurredAt) {
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(occurredAt))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.entry_dialog_date_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SnowGlassButton(
                    onClick = { showDatePicker = true },
                    blurScope = SnowBlurScope.OVERLAY
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(formattedDate)
                }
            }

            // Actions
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
                        val validPerson = if (creatingNewPerson) {
                            newPersonName.trim().isNotEmpty()
                        } else {
                            selectedPersonId != null
                        }
                        val parsedCents = AmountParser.parsePositiveCents(amountText)
                        personError = !validPerson
                        amountError = parsedCents == null
                        if (validPerson && parsedCents != null) {
                            onSave(
                                editingEntry?.id,
                                if (creatingNewPerson) null else selectedPersonId,
                                newPersonName.trim(),
                                newPersonAvatarType,
                                newPersonIconKey,
                                newPersonPhotoUri,
                                entryKind,
                                direction,
                                parsedCents,
                                noteText,
                                occurredAt
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { occurredAt = it }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
