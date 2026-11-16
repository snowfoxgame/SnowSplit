package com.snowsplit.app.feature.bubbles

import android.os.SystemClock
import android.os.Build
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.snowsplit.app.R
import com.snowsplit.app.core.common.CurrencyFormatter
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.components.SnowBlurScope
import com.snowsplit.app.core.designsystem.components.SnowGlassButton
import com.snowsplit.app.core.designsystem.components.SnowGlassCard
import com.snowsplit.app.core.designsystem.components.SnowGlassChip
import com.snowsplit.app.core.ui.AvatarBadge
import com.snowsplit.app.core.ui.MeProfileEditorDialog
import com.snowsplit.app.core.ui.PersonAvatarBadge
import com.snowsplit.app.core.ui.PersonEditorDialog
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.PersonBubble
import com.snowsplit.app.feature.entries.EntryEditorDialog
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun BubblesScreen(
    settings: AppSettings,
    isActivePage: Boolean,
    onToggleCalculationMode: (DebtCalculationMode) -> Unit,
    onOpenPersonEntries: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BubblesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tokens = SnowDosThemeTokens.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var selectedPersonIdForDialog by remember { mutableStateOf<Long?>(null) }
    var showMeProfileDialog by remember { mutableStateOf(false) }
    var showPersonEditorDialog by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<PersonBubble?>(null) }
    var showNewEntryDialog by remember { mutableStateOf(false) }

    val selectedSummary = remember(uiState.summaries, selectedPersonIdForDialog) {
        selectedPersonIdForDialog?.let { id ->
            uiState.summaries.firstOrNull { it.person.id == id }
        }
    }

    val simulation = remember { BubblePhysicsSimulation() }
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var headerHeightPx by remember { mutableIntStateOf(0) }

    val statusBarTopPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    val meDiameter = 98.dp
    val contactDiameter = 82.dp
    val meRadiusPx = with(density) { (meDiameter * 0.5f).toPx() }
    val contactRadiusPx = with(density) { (contactDiameter * 0.5f).toPx() }
    val headerTopPadPx = with(density) { tokens.spacing.sm.toPx() }
    val headerGapPx = with(density) { 12.dp.toPx() }
    val bottomSafePx = with(density) { (tokens.spacing.floatingNavBottomClearance + 8.dp).toPx() }
    val sideMarginPx = with(density) { tokens.spacing.sm.toPx() }
    val lineClearancePx = with(density) { 56.dp.toPx() }
    val contactSpacingPx = with(density) { 18.dp.toPx() }
    val bobAmplitudePx = with(density) { 9.dp.toPx() }

    val resolvedHeaderHeightPx = if (headerHeightPx > 0) {
        headerHeightPx.toFloat()
    } else {
        with(density) { 108.dp.toPx() }
    }

    LaunchedEffect(
        viewportWidthPx,
        viewportHeightPx,
        resolvedHeaderHeightPx,
        statusBarTopPx
    ) {
        simulation.updateLayoutMetrics(
            viewportWidthPx = viewportWidthPx.toFloat(),
            viewportHeightPx = viewportHeightPx.toFloat(),
            topSafePx = statusBarTopPx + headerTopPadPx + resolvedHeaderHeightPx + headerGapPx,
            bottomSafePx = bottomSafePx,
            sideMarginPx = sideMarginPx,
            meRadiusPx = meRadiusPx,
            contactRadiusPx = contactRadiusPx,
            lineClearancePx = lineClearancePx,
            contactSpacingPx = contactSpacingPx,
            bobAmplitudePx = bobAmplitudePx
        )
    }

    LaunchedEffect(uiState.people) {
        simulation.syncPeople(uiState.people)
    }

    // 120Hz frame-clock simulation loop; paused cleanly when off-page or when app leaves foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, isActivePage) {
        if (!isActivePage) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastFrameNanos = 0L
            var totalSeconds = 0f
            while (true) {
                withFrameNanos { frameNanos ->
                    if (lastFrameNanos != 0L) {
                        val dt = (frameNanos - lastFrameNanos) / 1_000_000_000f
                        totalSeconds += dt.coerceIn(0f, 0.05f)
                        simulation.step(dtSeconds = dt, totalTimeSeconds = totalSeconds)
                    }
                    lastFrameNanos = frameNanos
                }
            }
        }
    }

    val bubbleShape = if (tokens.surface.usePixelRetroStyle) {
        RoundedCornerShape(tokens.radius.card)
    } else {
        CircleShape
    }

    val overview = uiState.overview
    val balancedLabel = stringResource(R.string.bubbles_summary_balanced)
    val summaryAnnotated = buildAnnotatedString {
        when {
            overview.totalGreenCents == 0L && overview.totalRedCents == 0L -> {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append(balancedLabel)
                }
            }
            settings.debtCalculationMode == DebtCalculationMode.NET -> {
                if (overview.netCents >= 0L) {
                    val text = stringResource(
                        R.string.bubbles_summary_owed_to_you,
                        CurrencyFormatter.formatCents(overview.netCents, settings.currencyOption)
                    )
                    withStyle(SpanStyle(color = tokens.semantic.positive)) { append(text) }
                } else {
                    val text = stringResource(
                        R.string.bubbles_summary_you_owe,
                        CurrencyFormatter.formatCents(-overview.netCents, settings.currencyOption)
                    )
                    withStyle(SpanStyle(color = tokens.semantic.negative)) { append(text) }
                }
            }
            else -> {
                if (overview.totalGreenCents > 0L) {
                    val green = CurrencyFormatter.formatCents(overview.totalGreenCents, settings.currencyOption)
                    withStyle(SpanStyle(color = tokens.semantic.positive)) { append("+$green") }
                }
                if (overview.totalGreenCents > 0L && overview.totalRedCents > 0L) {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append("  ·  ")
                    }
                }
                if (overview.totalRedCents > 0L) {
                    val red = CurrencyFormatter.formatCents(overview.totalRedCents, settings.currencyOption)
                    withStyle(SpanStyle(color = tokens.semantic.negative)) { append("-$red") }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewportWidthPx = it.width
                viewportHeightPx = it.height
            }
    ) {
        // Static-coordinate hit & drag layer across the entire Bubble canvas (prevents drag oscillation).
        val summaries = uiState.summaries
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(summaries, meRadiusPx, contactRadiusPx) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPos = down.position
                            val pointerId: PointerId = down.id

                            val hitContact = summaries.asReversed().firstOrNull { s ->
                                val bx = simulation.positionX(s.person.id)
                                val by = simulation.positionY(s.person.id)
                                hypot(downPos.x - bx, downPos.y - by) <= contactRadiusPx + 6f
                            }
                            val hitMe = hitContact == null &&
                                hypot(downPos.x - simulation.centerX, downPos.y - simulation.centerY) <= meRadiusPx + 6f

                            if (hitContact == null && !hitMe) {
                                continue
                            }

                            var isDragging = false
                            var prevPos = downPos
                            var prevTimeMs = SystemClock.uptimeMillis()

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (change.changedToUpIgnoreConsumed()) {
                                    if (isDragging && hitContact != null) {
                                        simulation.endDrag(hitContact.person.id)
                                    } else if (!isDragging) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (hitContact != null) {
                                            selectedPersonIdForDialog = hitContact.person.id
                                        } else if (hitMe) {
                                            showMeProfileDialog = true
                                        }
                                    }
                                    change.consume()
                                    break
                                }

                                val currentPos = change.position
                                if (hitContact != null && !isDragging) {
                                    val distFromDown = hypot(
                                        currentPos.x - downPos.x,
                                        currentPos.y - downPos.y
                                    )
                                    if (distFromDown > touchSlop) {
                                        isDragging = true
                                        simulation.beginDrag(hitContact.person.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        prevPos = currentPos
                                        prevTimeMs = SystemClock.uptimeMillis()
                                        change.consume()
                                    }
                                } else if (isDragging && hitContact != null) {
                                    val dx = currentPos.x - prevPos.x
                                    val dy = currentPos.y - prevPos.y
                                    val nowMs = SystemClock.uptimeMillis()
                                    val dtSec = ((nowMs - prevTimeMs).coerceAtLeast(1L)) / 1000f
                                    simulation.dragBy(hitContact.person.id, dx, dy, dtSec)
                                    prevPos = currentPos
                                    prevTimeMs = nowMs
                                    change.consume()
                                }
                            }

                            if (isDragging && hitContact != null) {
                                simulation.endDrag(hitContact.person.id)
                            }
                        }
                    }
                }
        ) {
            if (summaries.isNotEmpty() && viewportWidthPx > 0 && viewportHeightPx > 0) {
                DualLineCanvas(
                    summaries = summaries,
                    currencyOption = settings.currencyOption,
                    simulation = simulation
                )
            }

            // Central "Me" Bubble
            if (viewportWidthPx > 0 && viewportHeightPx > 0) {
                Box(
                    modifier = Modifier
                        .size(meDiameter)
                        .offset {
                            IntOffset(
                                x = (simulation.centerX - meRadiusPx).roundToInt(),
                                y = (simulation.centerY - meRadiusPx).roundToInt()
                            )
                        }
                ) {
                    MeCenterBubble(
                        settings = settings,
                        shape = bubbleShape,
                        size = meDiameter
                    )
                }
            }

            // Use graphicsLayer GPU translation when live blur is disabled (API < 31, Battery Saver,
            // or 8-bit) to skip layout placement passes per frame; keep offset {} when Haze is active.
            val useLiveBlurLayout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !settings.reduceLiveBlur &&
                !tokens.surface.usePixelRetroStyle

            summaries.forEach { summary ->
                val person = summary.person
                key(person.id) {
                    val bubbleModifier = if (useLiveBlurLayout) {
                        Modifier
                            .offset {
                                @Suppress("UNUSED_VARIABLE")
                                val tick = simulation.frameVersion
                                IntOffset(
                                    x = (simulation.positionX(person.id) - contactRadiusPx).roundToInt(),
                                    y = (simulation.positionY(person.id) - contactRadiusPx).roundToInt()
                                )
                            }
                            .graphicsLayer {
                                @Suppress("UNUSED_VARIABLE")
                                val tick = simulation.frameVersion
                                val scale = if (simulation.isDragged(person.id)) 1.08f else 1f
                                scaleX = scale
                                scaleY = scale
                            }
                    } else {
                        Modifier.graphicsLayer {
                            @Suppress("UNUSED_VARIABLE")
                            val tick = simulation.frameVersion
                            translationX = simulation.positionX(person.id) - contactRadiusPx
                            translationY = simulation.positionY(person.id) - contactRadiusPx
                            val scale = if (simulation.isDragged(person.id)) 1.08f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                    ContactBubbleCard(
                        person = person,
                        diameter = contactDiameter,
                        shape = bubbleShape,
                        modifier = bubbleModifier
                    )
                }
            }
        }

        // Empty-state onboarding card anchored nicely below the Me bubble when no contacts exist yet
        if (summaries.isEmpty()) {
            SnowGlassCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = tokens.spacing.md,
                        end = tokens.spacing.md,
                        bottom = tokens.spacing.floatingNavBottomClearance + tokens.spacing.md
                    )
                    .fillMaxWidth(),
                shape = RoundedCornerShape(tokens.radius.card),
                blurScope = SnowBlurScope.BACKGROUND
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(tokens.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bubbles_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.bubbles_empty_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.bubbles_tap_me_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Floating Header Card (always above map & safe-measured for physics bounds)
        SnowGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = tokens.spacing.md,
                    end = tokens.spacing.md,
                    top = tokens.spacing.sm
                )
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height },
            shape = RoundedCornerShape(tokens.radius.card),
            blurScope = SnowBlurScope.BACKGROUND
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = tokens.spacing.md, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = summaryAnnotated,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    SnowGlassChip(
                        label = if (settings.debtCalculationMode == DebtCalculationMode.NET) {
                            stringResource(R.string.bubbles_mode_net)
                        } else {
                            stringResource(R.string.bubbles_mode_gross)
                        },
                        selected = true,
                        onClick = {
                            val next = if (settings.debtCalculationMode == DebtCalculationMode.NET) {
                                DebtCalculationMode.GROSS
                            } else {
                                DebtCalculationMode.NET
                            }
                            onToggleCalculationMode(next)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
                ) {
                    SnowGlassButton(
                        onClick = {
                            editingPerson = null
                            showPersonEditorDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "+ " + stringResource(R.string.bubbles_add_person_cta),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    SnowGlassButton(
                        onClick = { showNewEntryDialog = true },
                        highlighted = true,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.bubbles_add_entry_cta),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Dialog 1: Contact Bubble Quick-Action / Settlement Dialog
    BubbleActionDialog(
        summary = selectedSummary,
        settings = settings,
        onDismiss = { selectedPersonIdForDialog = null },
        onSaveEntry = viewModel::saveQuickEntry,
        onSettleAll = viewModel::settlePerson,
        onViewEntries = onOpenPersonEntries,
        onEditProfile = { person ->
            editingPerson = person
            showPersonEditorDialog = true
        }
    )

    // Dialog 2: Create / Edit Contact Person (Icon / Photo / Delete)
    val editingSummary = editingPerson?.let { person ->
        uiState.summaries.firstOrNull { it.person.id == person.id }
    }
    PersonEditorDialog(
        visible = showPersonEditorDialog,
        editingPerson = editingPerson,
        linkedEntryCount = editingSummary?.entryCount ?: 0,
        onImportPhoto = { uri ->
            viewModel.importAvatarPhoto(uri, previousLocalUri = editingPerson?.photoUri)
        },
        onDismiss = {
            showPersonEditorDialog = false
            editingPerson = null
        },
        onSave = { name, avatarType, iconKey, photoUri ->
            viewModel.savePerson(
                existingPerson = editingPerson,
                name = name,
                avatarType = avatarType,
                iconKey = iconKey,
                photoUri = photoUri
            )
        },
        onDelete = if (editingPerson != null) {
            { person -> viewModel.deletePerson(person) }
        } else {
            null
        }
    )

    // Dialog 3: Edit "Me" Profile (Name + Icon / Gallery Photo)
    MeProfileEditorDialog(
        visible = showMeProfileDialog,
        settings = settings,
        onImportPhoto = { uri ->
            viewModel.importAvatarPhoto(uri, previousLocalUri = settings.mePhotoUri)
        },
        onDismiss = { showMeProfileDialog = false },
        onSave = viewModel::setMeProfile
    )

    // Dialog 4: Quick "+ Eintrag" directly from Bubbles Header
    EntryEditorDialog(
        visible = showNewEntryDialog,
        editingEntry = null,
        people = uiState.people,
        currencyOption = settings.currencyOption,
        onImportAvatarPhoto = { uri -> viewModel.importAvatarPhoto(uri) },
        onDismiss = { showNewEntryDialog = false },
        onSave = viewModel::saveFullEntry
    )
}

@Composable
private fun MeCenterBubble(
    settings: AppSettings,
    shape: androidx.compose.ui.graphics.Shape,
    size: Dp
) {
    SnowGlassCard(
        modifier = Modifier.size(size),
        shape = shape,
        highlighted = true,
        blurScope = SnowBlurScope.BACKGROUND
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AvatarBadge(
                avatarType = settings.meAvatarType,
                iconKey = settings.meIconKey,
                photoUri = settings.mePhotoUri,
                contentDescription = settings.meDisplayName.ifBlank {
                    stringResource(R.string.bubbles_me_label)
                },
                size = 44.dp,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = settings.meDisplayName.ifBlank {
                    stringResource(R.string.bubbles_me_label)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ContactBubbleCard(
    person: PersonBubble,
    diameter: Dp,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    SnowGlassCard(
        modifier = modifier.size(diameter),
        shape = shape,
        blurScope = SnowBlurScope.BACKGROUND
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PersonAvatarBadge(
                person = person,
                size = 38.dp
            )
            Spacer(modifier = Modifier.size(3.dp))
            Text(
                text = person.name,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
