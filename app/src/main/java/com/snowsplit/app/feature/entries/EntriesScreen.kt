package com.snowsplit.app.feature.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowsplit.app.R
import com.snowsplit.app.core.common.CurrencyFormatter
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.components.SnowGlassButton
import com.snowsplit.app.core.designsystem.components.SnowGlassCard
import com.snowsplit.app.core.designsystem.components.SnowGlassChip
import com.snowsplit.app.core.designsystem.components.SnowGlassSnackbarHost
import com.snowsplit.app.core.designsystem.components.SnowSegmentOption
import com.snowsplit.app.core.designsystem.components.SnowSegmentedPill
import com.snowsplit.app.core.ui.PersonAvatarBadge
import com.snowsplit.app.domain.model.CurrencyOption
import com.snowsplit.app.domain.model.DebtDirection
import com.snowsplit.app.domain.model.DebtEntry
import com.snowsplit.app.domain.model.EntryKind
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun EntriesScreen(
    modifier: Modifier = Modifier,
    requestedPersonFilterId: Long? = null,
    requestedFilterNonce: Int = 0,
    viewModel: EntriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tokens = SnowDosThemeTokens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(requestedFilterNonce) {
        if (requestedFilterNonce > 0) {
            viewModel.setDirectionFilter(EntryDirectionFilter.ALL)
            viewModel.setSelectedPersonId(requestedPersonFilterId)
        }
    }

    var showEditor by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DebtEntry?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = tokens.spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.entries_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                SnowGlassButton(
                    onClick = {
                        editingEntry = null
                        showEditor = true
                    },
                    highlighted = true,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.entries_add_cta),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            SnowSegmentedPill(
                options = listOf(
                    SnowSegmentOption(EntryDirectionFilter.ALL, stringResource(R.string.entries_filter_all)),
                    SnowSegmentOption(EntryDirectionFilter.OWED_TO_ME, stringResource(R.string.entries_filter_owed_to_me)),
                    SnowSegmentOption(EntryDirectionFilter.I_OWE, stringResource(R.string.entries_filter_i_owe))
                ),
                selected = uiState.directionFilter,
                onSelect = viewModel::setDirectionFilter
            )

            if (uiState.people.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item(key = "filter_all_people", contentType = "person_filter_chip") {
                        SnowGlassChip(
                            label = stringResource(R.string.entries_filter_person_all),
                            selected = uiState.selectedPersonId == null,
                            onClick = { viewModel.setSelectedPersonId(null) }
                        )
                    }
                    items(
                        items = uiState.people,
                        key = { it.id },
                        contentType = { "person_filter_chip" }
                    ) { person ->
                        SnowGlassChip(
                            label = person.name,
                            selected = uiState.selectedPersonId == person.id,
                            onClick = {
                                val next = if (uiState.selectedPersonId == person.id) null else person.id
                                viewModel.setSelectedPersonId(next)
                            }
                        )
                    }
                }
            }

            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = tokens.spacing.floatingNavBottomClearance),
                    contentAlignment = Alignment.Center
                ) {
                    SnowGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(tokens.radius.card)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(tokens.spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.allEntriesCount == 0) {
                                    stringResource(R.string.entries_empty_title)
                                } else {
                                    stringResource(R.string.entries_filtered_empty_title)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (uiState.allEntriesCount == 0) {
                                    stringResource(R.string.entries_empty_subtitle)
                                } else {
                                    stringResource(R.string.entries_filtered_empty_subtitle)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = tokens.spacing.floatingNavBottomClearance
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.items,
                        key = { it.entry.id },
                        contentType = { "entry_row" }
                    ) { item ->
                        EntryRowCard(
                            item = item,
                            currencyOption = uiState.settings.currencyOption,
                            onEdit = {
                                editingEntry = item.entry
                                showEditor = true
                            },
                            onQuickSettle = {
                                val settleNote = item.entry.note?.takeIf { it.isNotBlank() }?.let {
                                    context.getString(R.string.entries_settled_note_prefix, it)
                                } ?: context.getString(R.string.entries_settled_note_default)
                                viewModel.quickSettleEntry(item, settleNote)
                                val formatted = CurrencyFormatter.formatCents(
                                    item.quickSettleAmountCents,
                                    uiState.settings.currencyOption
                                )
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.entries_settled_msg, formatted),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onDelete = {
                                val removed = item.entry
                                viewModel.deleteEntry(removed)
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.entries_deleted_msg),
                                        actionLabel = context.getString(R.string.entries_undo),
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreDeletedEntry(removed)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        SnowGlassSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = tokens.spacing.floatingNavBottomClearance + 8.dp)
        )
    }

    EntryEditorDialog(
        visible = showEditor,
        editingEntry = editingEntry,
        people = uiState.people,
        currencyOption = uiState.settings.currencyOption,
        onImportAvatarPhoto = viewModel::importAvatarPhoto,
        onDismiss = {
            showEditor = false
            editingEntry = null
        },
        onSave = viewModel::saveEntry
    )
}

@Composable
private fun EntryRowCard(
    item: EntryListItem,
    currencyOption: CurrencyOption,
    onEdit: () -> Unit,
    onQuickSettle: () -> Unit,
    onDelete: () -> Unit
) {
    val tokens = SnowDosThemeTokens.current
    val entry = item.entry
    val isGreen = entry.direction == DebtDirection.THEY_OWE_ME
    val amountColor = if (isGreen) tokens.semantic.positive else tokens.semantic.negative

    val directionLabel = when (entry.entryKind) {
        EntryKind.DEBT -> if (isGreen) {
            stringResource(R.string.entries_dir_they_owe_me)
        } else {
            stringResource(R.string.entries_dir_i_owe_them)
        }
        EntryKind.PAYMENT -> if (isGreen) {
            stringResource(R.string.entries_dir_they_paid_me)
        } else {
            stringResource(R.string.entries_dir_i_paid_them)
        }
    }

    val formattedAmount = remember(entry.amountCents, currencyOption) {
        CurrencyFormatter.formatCents(entry.amountCents, currencyOption)
    }
    val signedAmount = if (entry.entryKind == EntryKind.PAYMENT) {
        "✓ $formattedAmount"
    } else if (isGreen) {
        "+$formattedAmount"
    } else {
        "-$formattedAmount"
    }
    val formattedDate = remember(entry.occurredAt) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.occurredAt))
    }

    SnowGlassCard(
        shape = RoundedCornerShape(tokens.radius.card),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = tokens.spacing.md, end = tokens.spacing.sm, top = 14.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PersonAvatarBadge(
                    person = item.person,
                    size = 44.dp,
                    tint = amountColor
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.person.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$directionLabel · $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!entry.note.isNullOrBlank()) {
                        Text(
                            text = entry.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = signedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.canQuickSettle) {
                    TextButton(onClick = onQuickSettle) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(stringResource(R.string.entries_action_settle))
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.entries_action_edit),
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.entries_action_delete),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
