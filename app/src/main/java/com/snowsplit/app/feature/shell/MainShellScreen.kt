package com.snowsplit.app.feature.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.snowsplit.app.R
import com.snowsplit.app.core.designsystem.components.FloatingNavIconAnimation
import com.snowsplit.app.core.designsystem.components.FloatingNavItem
import com.snowsplit.app.core.designsystem.components.FloatingPillNavBar
import com.snowsplit.app.core.designsystem.components.LocalAppHazeState
import com.snowsplit.app.core.designsystem.components.snowPressFeedback
import com.snowsplit.app.domain.model.AppSettings
import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.TabTransitionMode
import com.snowsplit.app.feature.account.AccountScreen
import com.snowsplit.app.feature.bubbles.BubblesScreen
import com.snowsplit.app.feature.entries.EntriesScreen
import com.snowsplit.app.feature.settings.AppearanceSettingsScreen
import com.snowsplit.app.feature.settings.SettingsScreen
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.tanh

private enum class SettingsSubpage {
    NONE,
    APPEARANCE,
    ACCOUNT
}

@Composable
fun MainShellScreen(
    settings: AppSettings,
    onToggleCalculationMode: (DebtCalculationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val showAccountTab = remember(
        settings.accountTabVisibility,
        configuration.screenWidthDp,
        density.fontScale
    ) {
        settings.accountTabVisibility.isVisibleInNavBar(
            screenWidthDp = configuration.screenWidthDp,
            fontScale = density.fontScale
        )
    }

    val destinations = remember(showAccountTab) {
        buildList {
            add(AppTab.ENTRIES)
            add(AppTab.BUBBLES)
            if (showAccountTab) add(AppTab.ACCOUNT)
            add(AppTab.SETTINGS)
        }
    }

    val initialBubblesIndex = remember { destinations.indexOf(AppTab.BUBBLES).coerceAtLeast(0) }
    val pagerState = rememberPagerState(
        initialPage = initialBubblesIndex,
        pageCount = { destinations.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val appHazeState = LocalAppHazeState.current
    var activeSubpage by remember { mutableStateOf(SettingsSubpage.NONE) }
    var requestedPersonFilterId by remember { mutableStateOf<Long?>(null) }
    var requestedFilterNonce by remember { mutableIntStateOf(0) }

    var previousDestinations by remember { mutableStateOf(destinations) }
    val lastSelectedTab = remember { mutableStateOf(AppTab.BUBBLES) }
    LaunchedEffect(pagerState.settledPage) {
        val current = destinations.getOrNull(pagerState.settledPage)
        if (current != null) {
            lastSelectedTab.value = current
        }
    }
    if (destinations != previousDestinations) {
        val tabToKeep = previousDestinations.getOrNull(pagerState.currentPage)
            ?: lastSelectedTab.value
        val targetIndex = destinations.indexOf(tabToKeep)
        SideEffect {
            previousDestinations = destinations
            lastSelectedTab.value = tabToKeep
            if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                pagerState.requestScrollToPage(targetIndex)
            }
        }
    }
    val animateTabTransitions = settings.tabTransitionMode == TabTransitionMode.ANIMATED

    val edgeOverscrollOffset = remember { Animatable(0f) }
    val maxPullPx = with(density) { 48.dp.toPx() }
    val edgeOverscrollConnection = remember(pagerState, maxPullPx, coroutineScope) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = edgeOverscrollOffset.value
                if (current != 0f && source == NestedScrollSource.UserInput && available.x != 0f) {
                    val isDraggingBack = sign(available.x) != sign(current)
                    if (isDraggingBack) {
                        val rawNext = current + available.x * 0.55f
                        val next = if (sign(rawNext) != sign(current)) 0f else rawNext
                        coroutineScope.launch { edgeOverscrollOffset.snapTo(next) }
                        return Offset(x = available.x, y = 0f)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.x != 0f) {
                    val atStart = pagerState.currentPage == 0 && pagerState.currentPageOffsetFraction <= 0f
                    val atEnd = pagerState.currentPage == pagerState.pageCount - 1 &&
                        pagerState.currentPageOffsetFraction >= 0f
                    if ((atStart && available.x > 0f) || (atEnd && available.x < 0f)) {
                        val delta = available.x * 0.45f
                        val nextRaw = edgeOverscrollOffset.value + delta
                        val damped = maxPullPx * tanh(nextRaw / maxPullPx)
                        coroutineScope.launch { edgeOverscrollOffset.snapTo(damped) }
                        return Offset(x = available.x, y = 0f)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (abs(edgeOverscrollOffset.value) > 0.5f) {
                    edgeOverscrollOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                return Velocity.Zero
            }
        }
    }

    AnimatedContent(
        targetState = activeSubpage,
        transitionSpec = {
            if (targetState != SettingsSubpage.NONE) {
                (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 5 } +
                    fadeIn(tween(200))) togetherWith
                    fadeOut(tween(160))
            } else {
                fadeIn(tween(200)) togetherWith
                    (slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 5 } +
                        fadeOut(tween(160)))
            }
        },
        label = "shellSubpageTransition",
        modifier = modifier.fillMaxSize()
    ) { subpage ->
        when (subpage) {
            SettingsSubpage.APPEARANCE -> AppearanceSettingsScreen(
                onBack = { activeSubpage = SettingsSubpage.NONE }
            )
            SettingsSubpage.ACCOUNT -> AccountSubpageScreen(
                onBack = { activeSubpage = SettingsSubpage.NONE }
            )
            SettingsSubpage.NONE -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (appHazeState != null) {
                                    Modifier.hazeSource(state = appHazeState, zIndex = 1f)
                                } else {
                                    Modifier
                                }
                            )
                            .nestedScroll(edgeOverscrollConnection)
                            .graphicsLayer {
                                translationX = edgeOverscrollOffset.value
                            }
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            key = { index -> destinations.getOrNull(index)?.name ?: index },
                            modifier = Modifier.fillMaxSize()
                        ) { pageIndex ->
                            when (destinations[pageIndex]) {
                                AppTab.ENTRIES -> EntriesScreen(
                                    requestedPersonFilterId = requestedPersonFilterId,
                                    requestedFilterNonce = requestedFilterNonce
                                )
                                AppTab.BUBBLES -> BubblesScreen(
                                    settings = settings,
                                    isActivePage = pagerState.currentPage == pageIndex &&
                                        activeSubpage == SettingsSubpage.NONE,
                                    onToggleCalculationMode = onToggleCalculationMode,
                                    onOpenPersonEntries = { personId ->
                                        requestedPersonFilterId = personId
                                        requestedFilterNonce++
                                        val entriesIndex = destinations.indexOf(AppTab.ENTRIES)
                                        if (entriesIndex >= 0) {
                                            coroutineScope.launch {
                                                if (animateTabTransitions) {
                                                    pagerState.animateScrollToPage(
                                                        page = entriesIndex,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.88f,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                } else {
                                                    pagerState.scrollToPage(entriesIndex)
                                                }
                                            }
                                        }
                                    }
                                )
                                AppTab.ACCOUNT -> AccountScreen()
                                AppTab.SETTINGS -> SettingsScreen(
                                    onOpenAppearance = { activeSubpage = SettingsSubpage.APPEARANCE },
                                    onOpenAccount = {
                                        val accountIndex = destinations.indexOf(AppTab.ACCOUNT)
                                        if (accountIndex >= 0) {
                                            coroutineScope.launch {
                                                if (animateTabTransitions) {
                                                    pagerState.animateScrollToPage(
                                                        page = accountIndex,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.88f,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                } else {
                                                    pagerState.scrollToPage(accountIndex)
                                                }
                                            }
                                        } else {
                                            activeSubpage = SettingsSubpage.ACCOUNT
                                        }
                                    }
                                )
                            }
                        }
                    }

                    ShellFloatingNavBar(
                        destinations = destinations,
                        pagerState = pagerState,
                        animateTransitions = animateTabTransitions,
                        onSelectTab = { tab ->
                            val targetIndex = destinations.indexOf(tab)
                            if (targetIndex >= 0) {
                                coroutineScope.launch {
                                    if (animateTabTransitions) {
                                        pagerState.animateScrollToPage(
                                            page = targetIndex,
                                            animationSpec = spring(
                                                dampingRatio = 0.88f,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    } else {
                                        pagerState.scrollToPage(targetIndex)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSubpageScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    val backInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onBack,
                        interactionSource = backInteraction,
                        modifier = Modifier.snowPressFeedback(backInteraction, pressedScale = 0.92f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        AccountScreen(
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun ShellFloatingNavBar(
    destinations: List<AppTab>,
    pagerState: PagerState,
    animateTransitions: Boolean,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val entriesLabel = stringResource(R.string.nav_entries)
    val bubblesLabel = stringResource(R.string.nav_bubbles)
    val accountLabel = stringResource(R.string.nav_account)
    val settingsLabel = stringResource(R.string.nav_settings)

    val navItems = remember(destinations, entriesLabel, bubblesLabel, accountLabel, settingsLabel) {
        destinations.map { tab ->
            when (tab) {
                AppTab.ENTRIES -> FloatingNavItem(
                    destination = AppTab.ENTRIES,
                    label = entriesLabel,
                    selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                    unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    iconAnimation = FloatingNavIconAnimation.LEDGER_BARS
                )
                AppTab.BUBBLES -> FloatingNavItem(
                    destination = AppTab.BUBBLES,
                    label = bubblesLabel,
                    selectedIcon = Icons.Filled.BubbleChart,
                    unselectedIcon = Icons.Outlined.BubbleChart,
                    iconAnimation = FloatingNavIconAnimation.BUBBLES_PULSE
                )
                AppTab.ACCOUNT -> FloatingNavItem(
                    destination = AppTab.ACCOUNT,
                    label = accountLabel,
                    selectedIcon = Icons.Filled.AccountCircle,
                    unselectedIcon = Icons.Outlined.AccountCircle,
                    iconAnimation = FloatingNavIconAnimation.ACCOUNT_NOD
                )
                AppTab.SETTINGS -> FloatingNavItem(
                    destination = AppTab.SETTINGS,
                    label = settingsLabel,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    iconAnimation = FloatingNavIconAnimation.SETTINGS_SPIN
                )
            }
        }
    }

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val selectedDestination by remember(destinations) {
        derivedStateOf {
            destinations.getOrElse(pagerState.targetPage) { AppTab.BUBBLES }
        }
    }
    val scrollPositionProvider = remember(pagerState) {
        { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }

    FloatingPillNavBar(
        items = navItems,
        selectedDestination = selectedDestination,
        onSelect = onSelectTab,
        scrollPositionProvider = scrollPositionProvider,
        isDragInProgress = isDragged || pagerState.isScrollInProgress,
        animateTransitions = animateTransitions,
        modifier = modifier
    )
}
