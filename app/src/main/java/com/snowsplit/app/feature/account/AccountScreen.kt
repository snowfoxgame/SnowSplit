package com.snowsplit.app.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snowsplit.app.R
import com.snowsplit.app.core.designsystem.SnowDosThemeTokens
import com.snowsplit.app.core.designsystem.components.SnowGlassCard
import com.snowsplit.app.core.designsystem.components.SnowGlassChip
import com.snowsplit.app.domain.repository.AccountStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tokens = SnowDosThemeTokens.current

    val statusBadgeLabel = when (uiState.status) {
        AccountStatus.LOCAL_ONLY -> stringResource(R.string.account_status_local_only)
    }

    val syncStatusText = uiState.lastSyncTimestamp?.let { timestamp ->
        val formatted = remember(timestamp) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(timestamp))
        }
        stringResource(R.string.account_pillar_sync_last, formatted)
    } ?: stringResource(R.string.account_pillar_sync_never)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = tokens.spacing.md,
                end = tokens.spacing.md,
                top = tokens.spacing.md,
                bottom = tokens.spacing.floatingNavBottomClearance
            ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)
    ) {
        Text(
            text = stringResource(R.string.account_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(0.5f))

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
                    imageVector = Icons.Outlined.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                SnowGlassChip(
                    label = "${stringResource(R.string.account_coming_soon_badge)} · $statusBadgeLabel",
                    selected = true,
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.account_coming_soon_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.account_coming_soon_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        AccountInfoRowCard(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.account_pillar_storage_title),
            description = stringResource(R.string.account_pillar_storage_desc)
        )

        AccountInfoRowCard(
            icon = Icons.Outlined.Devices,
            title = stringResource(R.string.account_pillar_sync_title),
            description = syncStatusText
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AccountInfoRowCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    val tokens = SnowDosThemeTokens.current
    SnowGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.compactCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.md, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
