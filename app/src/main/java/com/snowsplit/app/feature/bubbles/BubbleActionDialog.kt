package com.snowsplit.app.feature.bubbles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import com.snowsplit.app.core.ui.PersonAvatarBadge
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.DebtDirection
import com.snowsplit.app.domain.model.EntryKind
import com.snowsplit.app.domain.model.PersonBalanceSummary
import com.snowsplit.app.domain.model.PersonBubble
import com.snowsplit.app.domain.model.SettleTarget

private enum class BubbleQuickAction {
    I_PAID,
    THEY_PAID,
    NEW_DEBT
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BubbleActionDialog(
    summary: PersonBalanceSummary?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSaveEntry: (
        personId: Long,
        entryKind: EntryKind,
        direction: DebtDirection,
        amountCents: Long,
        note: String?
    ) -> Unit,
    onSettleAll: (personId: Long, target: SettleTarget, note: String) -> Unit,
    onViewEntries: (personId: Long) -> Unit,
    onEditProfile: (PersonBubble) -> Unit
) {
    val tokens = SnowDosThemeTokens.current
    val visible = summary != null

    SnowFrostedDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        maxWidth = 460.dp
    ) {
        if (summary == null) return@SnowFrostedDialog
        val person = summary.person

        val defaultAction = remember(person.id, summary.displayGreenCents, summary.displayRedCents) {
            when {
                summary.displayRedCents > 0L && summary.displayGreenCents == 0L ->
                    BubbleQuickAction.I_PAID
                summary.displayGreenCents > 0L && summary.displayRedCents == 0L ->
                    BubbleQuickAction.THEY_PAID
                summary.displayRedCents > 0L || summary.displayGreenCents > 0L ->
                    if (summary.displayRedCents >= summary.displayGreenCents) {
                        BubbleQuickAction.I_PAID
                    } else {
                        BubbleQuickAction.THEY_PAID
                    }
                else -> BubbleQuickAction.NEW_DEBT
            }
        }

        var selectedAction by remember(person.id, visible) { mutableStateOf(defaultAction) }
        var newDebtDirection by remember(person.id, visible) { mutableStateOf(DebtDirection.THEY_OWE_ME) }
        var amountText by remember(person.id, visible) { mutableStateOf("") }
        var noteText by remember(person.id, visible) { mutableStateOf("") }
        var amountError by remember(person.id, visible) { mutableStateOf(false) }

        val currencySymbol = remember(settings.currencyOption) {
            CurrencyFormatter.resolveEffectiveCurrency(settings.currencyOption).symbol ?: "€"
        }

        val statusText = when {
            summary.displayGreenCents > 0L && summary.displayRedCents > 0L -> {
                stringResource(
                    R.string.bubbles_status_gross_pair,
                    CurrencyFormatter.formatCents(summary.displayGreenCents, settings.currencyOption),
                    CurrencyFormatter.formatCents(summary.displayRedCents, settings.currencyOption)
                )
            }
            summary.displayGreenCents > 0L -> {
                stringResource(
                    R.string.bubbles_status_owes_you,
                    CurrencyFormatter.formatCents(summary.displayGreenCents, settings.currencyOption)
                )
            }
            summary.displayRedCents > 0L -> {
                stringResource(
                    R.string.bubbles_status_you_owe,
                    CurrencyFormatter.formatCents(summary.displayRedCents, settings.currencyOption)
                )
            }
            else -> stringResource(R.string.bubbles_summary_balanced)
        }

        val statusColor = when {
            summary.displayGreenCents > 0L && summary.displayRedCents == 0L -> tokens.semantic.positive
            summary.displayRedCents > 0L && summary.displayGreenCents == 0L -> tokens.semantic.negative
            summary.displayGreenCents > 0L || summary.displayRedCents > 0L -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        val quickSettleCents = when (selectedAction) {
            BubbleQuickAction.I_PAID -> {
                if (settings.debtCalculationMode == DebtCalculationMode.NET) {
                    summary.displayRedCents
                } else {
                    summary.rawIOweThemCents
                }
            }
            BubbleQuickAction.THEY_PAID -> {
                if (settings.debtCalculationMode == DebtCalculationMode.NET) {
                    summary.displayGreenCents
                } else {
                    summary.rawTheyOweMeCents
                }
            }
            BubbleQuickAction.NEW_DEBT -> 0L
        }

        val showSettleAllBothChip = selectedAction != BubbleQuickAction.NEW_DEBT &&
            summary.rawTheyOweMeCents > 0L &&
            summary.rawIOweThemCents > 0L
        val settledNoteDefault = stringResource(R.string.entries_settled_note_default)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
        ) {
            // Header: Avatar + Name + Current Status + Quick Profile/Entries Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PersonAvatarBadge(
                    person = person,
                    size = 52.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            // 3 Quick Actions
            SnowSegmentedPill(
                options = listOf(
                    SnowSegmentOption(BubbleQuickAction.I_PAID, stringResource(R.string.bubbles_action_i_paid)),
                    SnowSegmentOption(BubbleQuickAction.THEY_PAID, stringResource(R.string.bubbles_action_they_paid)),
                    SnowSegmentOption(BubbleQuickAction.NEW_DEBT, stringResource(R.string.bubbles_action_new_debt))
                ),
                selected = selectedAction,
                onSelect = {
                    selectedAction = it
                    amountError = false
                },
                blurScope = SnowBlurScope.OVERLAY
            )

            // Direction chooser when adding a new debt
            if (selectedAction == BubbleQuickAction.NEW_DEBT) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SnowGlassChip(
                        label = stringResource(R.string.entry_dialog_debt_they_owe_me, person.name),
                        selected = newDebtDirection == DebtDirection.THEY_OWE_ME,
                        blurScope = SnowBlurScope.OVERLAY,
                        onClick = { newDebtDirection = DebtDirection.THEY_OWE_ME }
                    )
                    SnowGlassChip(
                        label = stringResource(R.string.entry_dialog_debt_i_owe_them, person.name),
                        selected = newDebtDirection == DebtDirection.I_OWE_THEM,
                        blurScope = SnowBlurScope.OVERLAY,
                        onClick = { newDebtDirection = DebtDirection.I_OWE_THEM }
                    )
                }
            }

            // Quick-settle chips when open debt exists
            if (quickSettleCents > 0L || showSettleAllBothChip) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (quickSettleCents > 0L) {
                        val formatted = CurrencyFormatter.formatCents(quickSettleCents, settings.currencyOption)
                        SnowGlassChip(
                            label = stringResource(R.string.bubbles_settle_all_chip, formatted),
                            selected = AmountParser.parsePositiveCents(amountText) == quickSettleCents,
                            blurScope = SnowBlurScope.OVERLAY,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                amountText = AmountParser.formatInputFromCents(quickSettleCents)
                                amountError = false
                            }
                        )
                    }
                    if (showSettleAllBothChip) {
                        SnowGlassChip(
                            label = stringResource(R.string.bubbles_settle_all_both_chip),
                            selected = false,
                            blurScope = SnowBlurScope.OVERLAY,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                onSettleAll(
                                    person.id,
                                    SettleTarget.ALL,
                                    noteText.trim().ifEmpty { settledNoteDefault }
                                )
                                onDismiss()
                            }
                        )
                    }
                }
            }

            // Amount input
            OutlinedTextField(
                value = amountText,
                onValueChange = { next ->
                    if (next.length <= 14) {
                        amountText = next
                        amountError = false
                    }
                },
                label = { Text(stringResource(R.string.entry_dialog_amount_label, currencySymbol)) },
                isError = amountError,
                supportingText = if (amountError) {
                    { Text(stringResource(R.string.entry_dialog_amount_error)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(tokens.radius.chip),
                modifier = Modifier.fillMaxWidth()
            )

            // Optional note input
            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 120) noteText = it },
                label = { Text(stringResource(R.string.entry_dialog_note_label)) },
                singleLine = true,
                shape = RoundedCornerShape(tokens.radius.chip),
                modifier = Modifier.fillMaxWidth()
            )

            // Secondary navigation & profile actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onViewEntries(person.id)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.bubbles_view_entries_cta),
                        maxLines = 1
                    )
                }

                TextButton(
                    onClick = {
                        onDismiss()
                        onEditProfile(person)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.bubbles_edit_profile_cta),
                        maxLines = 1
                    )
                }
            }

            // Primary Cancel / Save buttons
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
                        val cents = AmountParser.parsePositiveCents(amountText)
                        if (cents == null || cents <= 0L) {
                            amountError = true
                            return@SnowGlassButton
                        }
                        val (kind, direction) = when (selectedAction) {
                            BubbleQuickAction.I_PAID -> EntryKind.PAYMENT to DebtDirection.I_OWE_THEM
                            BubbleQuickAction.THEY_PAID -> EntryKind.PAYMENT to DebtDirection.THEY_OWE_ME
                            BubbleQuickAction.NEW_DEBT -> EntryKind.DEBT to newDebtDirection
                        }
                        onSaveEntry(
                            person.id,
                            kind,
                            direction,
                            cents,
                            noteText.trim().takeIf { it.isNotEmpty() }
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
