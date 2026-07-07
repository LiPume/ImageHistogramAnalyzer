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
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.model.HistogramExecutionEngine
import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import java.util.Locale

@Composable
fun PerformanceCard(
    strategy: HistogramCalculationStrategy,
    decodeTimeNanos: Long,
    metrics: HistogramPerformanceMetrics,
    modifier: Modifier = Modifier,
) {
    val calculationMillis = metrics.coreTotalNanos / NANOS_PER_MILLISECOND
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.selected_strategy), color = contentColor)
                Text(
                    text = stringResource(
                        when (strategy) {
                            HistogramCalculationStrategy.PRE_GRAYSCALE -> {
                                R.string.strategy_pre_grayscale
                            }

                            HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING -> {
                                R.string.strategy_while_counting
                            }
                        },
                    ),
                    color = contentColor,
                )
            }
            ValueRow(
                label = stringResource(R.string.execution_engine),
                value = stringResource(
                    when (metrics.executionEngine) {
                        HistogramExecutionEngine.KOTLIN_V2 -> R.string.engine_kotlin_v2
                        HistogramExecutionEngine.NATIVE_V3 -> R.string.engine_native_v3
                    },
                ),
                contentColor = contentColor,
            )
            ValueRow(
                label = stringResource(R.string.worker_count),
                value = metrics.workerCount.toString(),
                contentColor = contentColor,
            )
            TimeRow(
                label = stringResource(R.string.decode_time),
                nanos = decodeTimeNanos,
                contentColor = contentColor,
            )
            TimeRow(
                label = stringResource(R.string.pixel_access_time),
                nanos = metrics.pixelReadNanos,
                contentColor = contentColor,
            )
            if (strategy == HistogramCalculationStrategy.PRE_GRAYSCALE) {
                metrics.grayscaleConversionNanos?.let { grayscaleNanos ->
                    TimeRow(
                        label = stringResource(R.string.grayscale_conversion_time),
                        nanos = grayscaleNanos,
                        contentColor = contentColor,
                    )
                } ?: ValueRow(
                    label = stringResource(R.string.grayscale_conversion_time),
                    value = stringResource(R.string.not_available),
                    contentColor = contentColor,
                )
                TimeRow(
                    label = stringResource(R.string.histogram_counting_time),
                    nanos = metrics.countingNanos,
                    contentColor = contentColor,
                )
            } else {
                TimeRow(
                    label = stringResource(R.string.fused_grayscale_counting_time),
                    nanos = metrics.countingNanos,
                    contentColor = contentColor,
                )
            }
            TimeRow(
                label = stringResource(R.string.normalization_time),
                nanos = metrics.normalizationNanos,
                contentColor = contentColor,
            )
            TimeRow(
                label = stringResource(R.string.overhead_time),
                nanos = metrics.overheadNanos,
                contentColor = contentColor,
            )
            metrics.mergingNanos?.let { mergingNanos ->
                TimeRow(
                    label = stringResource(R.string.merge_time),
                    nanos = mergingNanos,
                    contentColor = contentColor,
                )
            } ?: ValueRow(
                label = stringResource(R.string.merge_time),
                value = stringResource(R.string.not_enabled),
                contentColor = contentColor,
            )
            TimeRow(
                label = stringResource(R.string.calculation_time),
                nanos = metrics.coreTotalNanos,
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
private fun ValueRow(
    label: String,
    value: String,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = contentColor)
        Text(value, color = contentColor)
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
