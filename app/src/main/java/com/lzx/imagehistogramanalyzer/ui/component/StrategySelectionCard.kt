package com.lzx.imagehistogramanalyzer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.strategy_title),
                style = MaterialTheme.typography.titleMedium,
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
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
