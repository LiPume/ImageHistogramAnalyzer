package com.lzx.imagehistogramanalyzer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy

@Composable
fun StrategySelectionCard(
    selectedStrategy: HistogramCalculationStrategy?,
    isProcessing: Boolean,
    onSelectStrategy: (HistogramCalculationStrategy) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.strategy_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.strategy_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StrategyChip(
                title = stringResource(R.string.strategy_pre_grayscale),
                description = stringResource(R.string.strategy_pre_grayscale_description),
                selected = selectedStrategy == HistogramCalculationStrategy.PRE_GRAYSCALE,
                enabled = !isProcessing,
                onClick = {
                    onSelectStrategy(HistogramCalculationStrategy.PRE_GRAYSCALE)
                },
            )
            StrategyChip(
                title = stringResource(R.string.strategy_while_counting),
                description = stringResource(R.string.strategy_while_counting_description),
                selected = selectedStrategy == HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING,
                enabled = !isProcessing,
                onClick = {
                    onSelectStrategy(HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING)
                },
            )
            Button(
                onClick = onCalculate,
                enabled = selectedStrategy != null && !isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.calculate_histogram))
            }
        }
    }
}

@Composable
private fun StrategyChip(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        label = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.82f),
                )
            }
        },
    )
}
