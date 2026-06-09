package com.cappsconsulting.prism.parentsuite.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.cappsconsulting.prism.engine.grounding.GroundingStatus
import com.cappsconsulting.prism.parentsuite.data.ConceptRecord
import com.cappsconsulting.prism.parentsuite.data.ConceptTileState
import com.cappsconsulting.prism.parentsuite.data.Pacing
import com.cappsconsulting.prism.parentsuite.viewmodel.ParentSuiteViewModel

/**
 * The concept Map — Doc 2.2 §1 + §2's grid of all concepts the child has encountered
 * or will encounter, all four [ConceptTileState]s rendered equally legibly. "Absence
 * shown as clearly as presence" (Principle 12): [ConceptTileState.ABSENT_BY_PARENT]
 * and [ConceptTileState.NOT_YET_REACHED] tiles are not hidden or deemphasized into
 * invisibility — a parent who can't see what's off the table isn't seeing the full map.
 *
 * Tapping a tile opens [ConceptDetailSheet] — all controls for that concept in one
 * place, including the counter-balance weight, which Doc 2.2 §4 designates a
 * "permanent glass-box control." It is in the sheet unconditionally: not behind
 * "Advanced" or conditionally surfaced only for power users.
 *
 * Hard Line 6 enforcement here: [GroundingStatus] renders as plain-language banded
 * text ("exploring", "getting it", "owns it") — never as a number, percentage, or bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: ParentSuiteViewModel, modifier: Modifier = Modifier) {
    val concepts by viewModel.concepts.collectAsState()
    var selectedConceptId by remember { mutableStateOf<String?>(null) }
    val selectedConcept = concepts.find { it.id == selectedConceptId }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(concepts, key = { it.id }) { concept ->
            ConceptTile(
                concept = concept,
                onClick = { selectedConceptId = concept.id },
            )
        }
    }

    if (selectedConcept != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedConceptId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ConceptDetailSheet(
                concept = selectedConcept,
                onPacingChange = { viewModel.setPacing(selectedConcept.id, it) },
                onCounterBalanceChange = { viewModel.setCounterBalanceWeight(selectedConcept.id, it) },
                onDormantChange = { viewModel.setDormant(selectedConcept.id, it) },
                onAbsentByParentChange = { viewModel.setAbsentByParent(selectedConcept.id, it) },
            )
        }
    }
}

@Composable
private fun ConceptTile(concept: ConceptRecord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    when (concept.tileState) {
        ConceptTileState.NOT_YET_REACHED -> OutlinedCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(120.dp),
        ) {
            TileContent(concept = concept)
        }

        ConceptTileState.DORMANT -> Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(120.dp).alpha(0.55f),
        ) {
            TileContent(concept = concept)
        }

        ConceptTileState.ABSENT_BY_PARENT -> Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
            modifier = modifier.fillMaxWidth().height(120.dp),
        ) {
            TileContent(concept = concept)
        }

        ConceptTileState.ACTIVE -> ElevatedCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(120.dp),
        ) {
            TileContent(concept = concept)
        }
    }
}

@Composable
private fun TileContent(concept: ConceptRecord, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = concept.label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
        )
        Column {
            Text(
                text = when (concept.tileState) {
                    ConceptTileState.ACTIVE -> concept.groundingStatus.displayLabel()
                    ConceptTileState.DORMANT -> "paused — ${concept.groundingStatus.displayLabel()}"
                    ConceptTileState.ABSENT_BY_PARENT -> "off limits"
                    ConceptTileState.NOT_YET_REACHED -> "not yet reached"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (concept.tileState == ConceptTileState.ACTIVE && concept.counterBalanceWeight > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Balance,
                        contentDescription = "counter-balance active",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = " weighted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConceptDetailSheet(
    concept: ConceptRecord,
    onPacingChange: (Pacing) -> Unit,
    onCounterBalanceChange: (Double) -> Unit,
    onDormantChange: (Boolean) -> Unit,
    onAbsentByParentChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
    ) {
        Text(
            text = concept.label,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "grounding: ${concept.groundingStatus.displayLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Pacing", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pacing.entries.forEach { pacing ->
                FilterChip(
                    selected = concept.pacing == pacing,
                    onClick = { onPacingChange(pacing) },
                    label = { Text(pacing.displayLabel) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Counter-balance weight", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Higher weight = Companion offers this topic more when the child seems to need grounding.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Slider(
            value = concept.counterBalanceWeight.toFloat(),
            onValueChange = { onCounterBalanceChange(it.toDouble()) },
            valueRange = 0f..5f,
            steps = 9,
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pause this concept", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Companion won't bring it up until you re-enable it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = concept.tileState == ConceptTileState.DORMANT,
                onCheckedChange = { onDormantChange(it) },
                enabled = concept.tileState != ConceptTileState.ABSENT_BY_PARENT,
            )
        }
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mark off limits",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Hard stop — Companion will never raise this topic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = concept.tileState == ConceptTileState.ABSENT_BY_PARENT,
                onCheckedChange = { onAbsentByParentChange(it) },
            )
        }
    }
}

private fun GroundingStatus.displayLabel(): String = when (this) {
    GroundingStatus.EXPLORING -> "exploring"
    GroundingStatus.GETTING_IT -> "getting it"
    GroundingStatus.OWNS_IT -> "owns it"
}
