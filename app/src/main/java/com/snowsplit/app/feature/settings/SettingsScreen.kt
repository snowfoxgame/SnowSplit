package com.snowsplit.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.BlurOff
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowsplit.app.BuildConfig
import com.snowsplit.app.R
import com.snowsplit.app.core.common.CurrencyFormatter
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.ThemeRegistry
import com.snowsplit.app.core.designsystem.components.SnowFrostedDialog
import com.snowsplit.app.core.designsystem.components.SnowGlassButton
import com.snowsplit.app.core.designsystem.components.SnowGlassCard
import com.snowsplit.app.core.designsystem.components.snowPressFeedback
import com.snowsplit.app.core.designsystem.titleRes
import com.snowsplit.app.core.ui.MeProfileEditorDialog
import com.snowsplit.app.domain.model.AppLanguage
import com.snowsplit.app.domain.model.CurrencyOption
import com.snowsplit.app.domain.model.DebtCalculationMode
import com.snowsplit.app.domain.model.TabTransitionMode

private enum class SettingsDialogType {
    CALCULATION_MODE,
    CURRENCY,
    MY_PROFILE,
    LANGUAGE,
    TAB_TRANSITION,
    HELP,
    ABOUT
}

@Composable
fun SettingsScreen(
    onOpenAppearance: () -> Unit,
    onOpenAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val tokens = SnowDosThemeTokens.current
    var activeDialog by remember { mutableStateOf<SettingsDialogType?>(null) }

    val resolvedAutoSymbol = remember(settings.currencyOption) {
        CurrencyFormatter.resolveEffectiveCurrency(CurrencyOption.AUTO).symbol ?: "€"
    }

    val calculationStatus = when (settings.debtCalculationMode) {
        DebtCalculationMode.NET -> stringResource(R.string.settings_calc_net)
        DebtCalculationMode.GROSS -> stringResource(R.string.settings_calc_gross)
    }

    val currencyStatus = when (settings.currencyOption) {
        CurrencyOption.AUTO -> stringResource(R.string.settings_currency_auto_status, resolvedAutoSymbol)
        CurrencyOption.EUR -> stringResource(R.string.settings_currency_eur)
        CurrencyOption.USD -> stringResource(R.string.settings_currency_usd)
        CurrencyOption.GBP -> stringResource(R.string.settings_currency_gbp)
    }

    val meNameResolved = settings.meDisplayName.ifBlank {
        stringResource(R.string.bubbles_me_label)
    }

    val activePreset = remember(settings.appearance.presetId, settings.appearance.autoSeasonalThemeEnabled) {
        ThemeRegistry.findPreset(settings.appearance.effectivePresetId())
    }
    val appearanceStatus = stringResource(activePreset.titleRes())

    val languageStatus = when (settings.appLanguage) {
        AppLanguage.DE -> stringResource(R.string.settings_language_de_status)
        AppLanguage.EN -> stringResource(R.string.settings_language_en_status)
    }

    val tabTransitionStatus = when (settings.tabTransitionMode) {
        TabTransitionMode.ANIMATED -> stringResource(R.string.settings_tab_transition_animated_status)
        TabTransitionMode.INSTANT -> stringResource(R.string.settings_tab_transition_instant_status)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = tokens.spacing.md,
            end = tokens.spacing.md,
            top = tokens.spacing.md,
            bottom = tokens.spacing.floatingNavBottomClearance
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "settings_title", contentType = "title") {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        // 1. Account
        item(key = "header_account", contentType = "header") {
            SettingsSectionHeader(text = stringResource(R.string.settings_section_account))
        }

        item(key = "row_account", contentType = "action") {
            SettingsActionRow(
                icon = Icons.Outlined.AccountCircle,
                title = stringResource(R.string.account_title),
                subtitle = stringResource(R.string.account_row_subtitle),
                highlighted = true,
                iconTint = MaterialTheme.colorScheme.primary,
                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onClick = onOpenAccount
            )
        }

        // 2. Splits & Profile
        item(key = "header_splits_profile", contentType = "header") {
            SettingsSectionHeader(
                text = stringResource(R.string.settings_header_splits_profile),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item(key = "row_calculation", contentType = "action") {
            SettingsActionRow(
                icon = Icons.Outlined.Balance,
                title = stringResource(R.string.settings_section_calculation),
                subtitle = calculationStatus,
                onClick = { activeDialog = SettingsDialogType.CALCULATION_MODE }
            )
        }

        item(key = "row_currency", contentType = "action") {
            SettingsActionRow(
                icon = Icons.Outlined.Payments,
                title = stringResource(R.string.settings_section_currency),
                subtitle = currencyStatus,
                onClick = { activeDialog = SettingsDialogType.CURRENCY }
            )
        }

        item(key = "row_my_profile", contentType = "action") {
            SettingsActionRow(
                icon = Icons.Outlined.Face,
                title = stringResource(R.string.settings_my_profile_title),
                subtitle = stringResource(R.string.settings_my_profile_status, meNameResolved),
                onClick = { activeDialog = SettingsDialogType.MY_PROFILE }
            )
        }

        // 3. Appearance
        item(key = "header_appearance", contentType = "header") {
            SettingsSectionHeader(
                text = stringResource(R.string.settings_header_appearance),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item(key = "row_appearance", contentType = "action") {
            SettingsActionRow(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_section_appearance),
                subtitle = appearanceStatus,
                onClick = onOpenAppearance
            )
        }

        item(key = "row_tab_transition", contentType = "action") {
            SettingsActionRow(
                icon = Icons.AutoMirrored.Outlined.CompareArrows,
                title = stringResource(R.string.settings_tab_transition_label),
                subtitle = tabTransitionStatus,
                onClick = { activeDialog = SettingsDialogType.TAB_TRANSITION }
            )
        }

        item(key = "row_reduce_live_blur", contentType = "switch") {
            SettingsSwitchRow(
                icon = Icons.Outlined.BlurOff,
                title = stringResource(R.string.settings_reduce_live_blur),
                subtitle = if (settings.reduceLiveBlur) {
                    stringResource(R.string.settings_reduce_live_blur_on)
                } else {
                    stringResource(R.string.settings_reduce_live_blur_off)
                },
                checked = settings.reduceLiveBlur,
                onCheckedChange = viewModel::setReduceLiveBlur
            )
        }

        item(key = "row_language", contentType = "action") {
            SettingsActionRow(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.settings_language_label),
                subtitle = languageStatus,
                onClick = { activeDialog = SettingsDialogType.LANGUAGE }
            )
        }

        // 4. Experimental
        item(key = "header_experimental", contentType = "header") {
            SettingsSectionHeader(
                text = stringResource(R.string.settings_header_experimental),
                badge = stringResource(R.string.settings_experimental_badge),
                subtitle = stringResource(R.string.settings_experimental_subtitle),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item(key = "row_cross_app_sync", contentType = "switch") {
            val isSyncActive = settings.partnerAppConnected && settings.crossAppSyncEnabled
            SettingsSwitchRow(
                icon = Icons.Outlined.Sync,
                title = stringResource(R.string.settings_cross_app_sync),
                subtitle = when {
                    !settings.partnerAppConnected ->
                        stringResource(R.string.settings_cross_app_sync_unavailable)
                    isSyncActive ->
                        stringResource(R.string.settings_cross_app_sync_on)
                    else ->
                        stringResource(R.string.settings_cross_app_sync_off)
                },
                checked = isSyncActive,
                enabled = settings.partnerAppConnected,
                highlighted = isSyncActive,
                onCheckedChange = viewModel::setCrossAppSyncEnabled
            )
        }

        item(key = "row_sync_app_icon", contentType = "switch") {
            SettingsSwitchRow(
                icon = Icons.Outlined.Apps,
                title = stringResource(R.string.settings_sync_app_icon),
                subtitle = if (settings.syncAppIconWithTheme) {
                    stringResource(R.string.settings_sync_app_icon_on)
                } else {
                    stringResource(R.string.settings_sync_app_icon_off)
                },
                checked = settings.syncAppIconWithTheme,
                onCheckedChange = viewModel::setSyncAppIconWithTheme
            )
        }

        // 5. Help & Info
        item(key = "header_help_info", contentType = "header") {
            SettingsSectionHeader(
                text = stringResource(R.string.settings_header_help_info),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item(key = "row_help", contentType = "action") {
            SettingsActionRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = stringResource(R.string.settings_help_title),
                subtitle = stringResource(R.string.settings_help_subtitle),
                onClick = { activeDialog = SettingsDialogType.HELP }
            )
        }

        item(key = "row_about", contentType = "action") {
            SettingsActionRow(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_about_title),
                subtitle = stringResource(
                    R.string.settings_about_subtitle,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                ),
                onClick = { activeDialog = SettingsDialogType.ABOUT }
            )
        }
    }

    // Dialog 1: Debt Calculation Mode
    SnowFrostedDialog(
        visible = activeDialog == SettingsDialogType.CALCULATION_MODE,
        onDismissRequest = { activeDialog = null }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.settings_section_calculation),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            DialogRadioOption(
                title = stringResource(R.string.settings_calc_net),
                description = stringResource(R.string.settings_calc_net_desc),
                selected = settings.debtCalculationMode == DebtCalculationMode.NET,
                onClick = {
                    viewModel.setDebtCalculationMode(DebtCalculationMode.NET)
                    activeDialog = null
                }
            )
            DialogRadioOption(
                title = stringResource(R.string.settings_calc_gross),
                description = stringResource(R.string.settings_calc_gross_desc),
                selected = settings.debtCalculationMode == DebtCalculationMode.GROSS,
                onClick = {
                    viewModel.setDebtCalculationMode(DebtCalculationMode.GROSS)
                    activeDialog = null
                }
            )
            DialogCloseRow(onClose = { activeDialog = null })
        }
    }

    // Dialog 2: Currency
    SnowFrostedDialog(
        visible = activeDialog == SettingsDialogType.CURRENCY,
        onDismissRequest = { activeDialog = null }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.settings_section_currency),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            DialogRadioOption(
                title = stringResource(R.string.settings_currency_auto),
                description = stringResource(R.string.settings_currency_auto_hint, resolvedAutoSymbol),
                selected = settings.currencyOption == CurrencyOption.AUTO,
                onClick = {
                    viewModel.setCurrencyOption(CurrencyOption.AUTO)
                    activeDialog = null
                }
            )
            DialogRadioOption(
                title = stringResource(R.string.settings_currency_eur),
                selected = settings.currencyOption == CurrencyOption.EUR,
                onClick = {
                    viewModel.setCurrencyOption(CurrencyOption.EUR)
                    activeDialog = null
                }
            )
            DialogRadioOption(
                title = stringResource(R.string.settings_currency_usd),
                selected = settings.currencyOption == CurrencyOption.USD,
                onClick = {
                    viewModel.setCurrencyOption(CurrencyOption.USD)
                    activeDialog = null
                }
            )
            DialogRadioOption(
                title = stringResource(R.string.settings_currency_gbp),
                selected = settings.currencyOption == CurrencyOption.GBP,
                onClick = {
                    viewModel.setCurrencyOption(CurrencyOption.GBP)
                    activeDialog = null
                }
            )
            DialogCloseRow(onClose = { activeDialog = null })
        }
    }

    // Dialog 3: My Profile ("Me" Bubble)
    MeProfileEditorDialog(
        visible = activeDialog == SettingsDialogType.MY_PROFILE,
        settings = settings,
        onImportPhoto = viewModel::importMeAvatarPhoto,
        onDismiss = { activeDialog = null },
        onSave = viewModel::setMeProfile
    )

    // Dialog 4: Language
    SnowFrostedDialog(
        visible = activeDialog == SettingsDialogType.LANGUAGE,
        onDismissRequest = { activeDialog = null }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.settings_language_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            DialogRadioOption(
                title = stringResource(R.string.settings_language_de_status),
                selected = settings.appLanguage == AppLanguage.DE,
                onClick = {
                    viewModel.setAppLanguage(AppLanguage.DE)
                    activeDialog = null
                }
            )
            DialogRadioOption(
                title = stringResource(R.string.settings_language_en_status),
                selected = settings.appLanguage == AppLanguage.EN,
                onClick = {
                    viewModel.setAppLanguage(AppLanguage.EN)
                    activeDialog = null
                }
            )
            DialogCloseRow(onClose = { activeDialog = null })
        }
    }

    // Dialog 4b: Tab Transition Mode
    SnowFrostedDialog(
        visible = activeDialog == SettingsDialogType.TAB_TRANSITION,
        onDismissRequest = { activeDialog = null }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.settings_tab_transition_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            DialogRadioOption(
                title = stringResource(R.string.settings_tab_transition_animated),
                description = stringResource(R.string.settings_tab_transition_animated_desc),
                selected = settings.tabTransitionMode == TabTransitionMode.ANIMATED,
                onClick = {
                    viewModel.setTabTransitionMode(TabTransitionMode.ANIMATED)
                    activeDialog = null
                }
            )
            DialogRadioOption(
                title = stringResource(R.string.settings_tab_transition_instant),
                description = stringResource(R.string.settings_tab_transition_instant_desc),
                selected = settings.tabTransitionMode == TabTransitionMode.INSTANT,
                onClick = {
                    viewModel.setTabTransitionMode(TabTransitionMode.INSTANT)
                    activeDialog = null
                }
            )
            DialogCloseRow(onClose = { activeDialog = null })
        }
    }

    // Dialog 6: Help & Tips
    SnowFrostedDialog(
        visible = activeDialog == SettingsDialogType.HELP,
        onDismissRequest = { activeDialog = null }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.settings_help_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_help_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DialogCloseRow(onClose = { activeDialog = null })
        }
    }

    // Dialog 7: App Version & Info
    SnowFrostedDialog(
        visible = activeDialog == SettingsDialogType.ABOUT,
        onDismissRequest = { activeDialog = null }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DialogCloseRow(onClose = { activeDialog = null })
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    subtitle: String? = null
) {
    val tokens = SnowDosThemeTokens.current
    Column(
        modifier = modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp, bottom = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(tokens.radius.pill),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingIcon: ImageVector? = null
) {
    val tokens = SnowDosThemeTokens.current
    val shape = RoundedCornerShape(tokens.radius.card)
    val interactionSource = remember { MutableInteractionSource() }

    SnowGlassCard(
        shape = shape,
        highlighted = highlighted,
        modifier = Modifier
            .fillMaxWidth()
            .snowPressFeedback(interactionSource, pressedScale = 0.985f)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.md, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    highlighted: Boolean = false
) {
    val tokens = SnowDosThemeTokens.current
    val shape = RoundedCornerShape(tokens.radius.card)
    val interactionSource = remember { MutableInteractionSource() }

    SnowGlassCard(
        shape = shape,
        highlighted = highlighted,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.snowPressFeedback(interactionSource, pressedScale = 0.985f)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.md, vertical = 12.dp)
                .alpha(if (enabled) 1f else 0.58f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun DialogCloseRow(onClose: () -> Unit) {
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        SnowGlassButton(onClick = onClose) {
            Text(text = stringResource(R.string.action_close))
        }
    }
}

@Composable
private fun DialogRadioOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = null
) {
    val tokens = SnowDosThemeTokens.current
    val shape = RoundedCornerShape(tokens.radius.compactCard)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        if (!description.isNullOrBlank()) {
            val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
            val titleWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = titleWeight)) {
                        append(title)
                    }
                    withStyle(SpanStyle(color = subtitleColor)) {
                        append(" — ")
                        append(description)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
