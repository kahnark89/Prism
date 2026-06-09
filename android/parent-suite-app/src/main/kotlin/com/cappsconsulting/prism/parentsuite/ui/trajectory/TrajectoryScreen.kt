package com.cappsconsulting.prism.parentsuite.ui.trajectory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cappsconsulting.prism.engine.grounding.GroundingStatus
import com.cappsconsulting.prism.parentsuite.viewmodel.ParentSuiteViewModel
import com.cappsconsulting.prism.sync.payload.SessionSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The learning Trajectory — Doc 2.2 §1's chronological narrative view of the
 * child's sessions. Milestone cards, plain language, no numeric scores.
 *
 * Hard Line 6 enforcement: grounding status renders as banded text only —
 * "exploring", "getting it", "owns it" — never as a confidence number, percentage,
 * or bar. A parent reading this screen sees the same banded language the Companion
 * uses internally; they do not see the raw model output that drove it.
 */
@Composable
fun TrajectoryScreen(viewModel: ParentSuiteViewModel, modifier: Modifier = Modifier) {
    val sessions by viewModel.recentSessions.collectAsState()

    if (sessions.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No sessions yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Once the Companion has had sessions with your child, each one will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(sessions, key = { it.sessionId }) { session ->
            SessionCard(session = session)
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = session.concept,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (session.isNewExposure) {
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            text = "first time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                } else if (session.isReappearance) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = "revisit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = GroundingStatus.fromWireValue(session.groundingStatus).displayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = session.startedAtEpochSeconds.toFormattedDateTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun GroundingStatus.displayLabel(): String = when (this) {
    GroundingStatus.EXPLORING -> "exploring"
    GroundingStatus.GETTING_IT -> "getting it"
    GroundingStatus.OWNS_IT -> "owns it"
}

private fun Double.toFormattedDateTime(): String = runCatching {
    Instant.ofEpochSecond(toLong())
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
}.getOrElse { "" }
