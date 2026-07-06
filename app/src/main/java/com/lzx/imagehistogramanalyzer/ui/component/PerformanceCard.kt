package com.lzx.imagehistogramanalyzer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import java.util.Locale

@Composable
fun PerformanceCard(
    decodeTimeNanos: Long,
    calculationTimeNanos: Long,
    modifier: Modifier = Modifier,
) {
    val calculationMillis = calculationTimeNanos / NANOS_PER_MILLISECOND
    val targetMet = calculationMillis < TARGET_MILLIS
    val containerColor = if (targetMet) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (targetMet) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.performance_title),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            TimeRow(
                label = stringResource(R.string.decode_time),
                nanos = decodeTimeNanos,
                contentColor = contentColor,
            )
            TimeRow(
                label = stringResource(R.string.calculation_time),
                nanos = calculationTimeNanos,
                contentColor = contentColor,
            )
            Text(
                text = stringResource(
                    if (targetMet) R.string.performance_target_met
                    else R.string.performance_target_not_met,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
            Text(
                text = stringResource(R.string.performance_scope),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun TimeRow(label: String, nanos: Long, contentColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = contentColor)
        Text(
            text = String.format(Locale.US, "%.3f ms", nanos / NANOS_PER_MILLISECOND),
            color = contentColor,
        )
    }
}

private const val NANOS_PER_MILLISECOND = 1_000_000.0
private const val TARGET_MILLIS = 300.0
