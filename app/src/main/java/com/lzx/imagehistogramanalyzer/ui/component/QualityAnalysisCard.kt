package com.lzx.imagehistogramanalyzer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.domain.insight.ImageInsightResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import java.util.Locale

@Composable
fun QualityAnalysisCard(
    result: ImageQualityResult,
    insight: ImageInsightResult? = null,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.quality_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            QualityValueRow(
                label = stringResource(R.string.quality_category),
                value = stringResource(result.category.labelResource),
                valueColor = MaterialTheme.colorScheme.primary,
            )
            ToneDistributionBar(result = result)
            QualityValueRow(
                label = stringResource(R.string.mean_gray),
                value = String.format(Locale.US, "%.2f", result.meanGray),
            )
            QualityValueRow(
                label = stringResource(R.string.dark_ratio),
                value = String.format(Locale.US, "%.1f%%", result.darkRatio * 100.0),
            )
            QualityValueRow(
                label = stringResource(R.string.bright_ratio),
                value = String.format(Locale.US, "%.1f%%", result.brightRatio * 100.0),
            )
            QualityValueRow(
                label = stringResource(R.string.gray_standard_deviation),
                value = String.format(Locale.US, "%.2f", result.standardDeviation),
            )
            Text(
                text = stringResource(R.string.quality_scope),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (insight != null) {
                HorizontalDivider()
                ImageInsightSection(insight = insight)
            }
        }
    }
}

@Composable
private fun QualityValueRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label, modifier = Modifier.weight(1.35f))
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToneDistributionBar(result: ImageQualityResult) {
    val darkRatio = result.darkRatio.coerceIn(0.0, 1.0)
    val brightRatio = result.brightRatio.coerceIn(0.0, 1.0)
    val midRatio = (1.0 - darkRatio - brightRatio).coerceIn(0.0, 1.0)
    val description = stringResource(
        R.string.tone_distribution_accessibility,
        darkRatio * 100.0,
        midRatio * 100.0,
        brightRatio * 100.0,
    )
    val darkColor = MaterialTheme.colorScheme.primary
    val midColor = MaterialTheme.colorScheme.tertiary
    val brightColor = MaterialTheme.colorScheme.secondary

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.tone_distribution),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = description },
        ) {
            if (darkRatio > 0.0) {
                Box(
                    Modifier
                        .weight(darkRatio.toFloat())
                        .fillMaxHeight()
                        .background(darkColor),
                )
            }
            if (midRatio > 0.0) {
                Box(
                    Modifier
                        .weight(midRatio.toFloat())
                        .fillMaxHeight()
                        .background(midColor),
                )
            }
            if (brightRatio > 0.0) {
                Box(
                    Modifier
                        .weight(brightRatio.toFloat())
                        .fillMaxHeight()
                        .background(brightColor),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ToneLegend(stringResource(R.string.dark_ratio), darkColor)
            ToneLegend(stringResource(R.string.mid_tone_ratio), midColor)
            ToneLegend(stringResource(R.string.bright_ratio), brightColor)
        }
    }
}

@Composable
private fun ToneLegend(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color),
        )
        Text(text = label.substringBefore('（'), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ImageInsightSection(insight: ImageInsightResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.insight_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.insight_basis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InsightParagraph(
            label = stringResource(R.string.insight_summary_label),
            text = insight.summary,
        )
        Text(
            text = insight.brightnessDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = insight.exposureDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = insight.contrastDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = insight.colorDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InsightParagraph(
            label = stringResource(R.string.insight_advice_label),
            text = insight.advice,
        )
    }
}

@Composable
private fun InsightParagraph(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val ImageQualityCategory.labelResource: Int
    get() = when (this) {
        ImageQualityCategory.DARK -> R.string.quality_dark
        ImageQualityCategory.BRIGHT -> R.string.quality_bright
        ImageQualityCategory.LOW_CONTRAST -> R.string.quality_low_contrast
        ImageQualityCategory.NORMAL -> R.string.quality_normal
    }
