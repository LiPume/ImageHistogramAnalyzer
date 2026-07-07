package com.lzx.imagehistogramanalyzer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import java.util.Locale

@Composable
fun QualityAnalysisCard(
    result: ImageQualityResult,
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
            )
            QualityValueRow(
                label = stringResource(R.string.quality_category),
                value = stringResource(result.category.labelResource),
                valueColor = MaterialTheme.colorScheme.primary,
            )
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(text = value, color = valueColor)
    }
}

private val ImageQualityCategory.labelResource: Int
    get() = when (this) {
        ImageQualityCategory.DARK -> R.string.quality_dark
        ImageQualityCategory.BRIGHT -> R.string.quality_bright
        ImageQualityCategory.LOW_CONTRAST -> R.string.quality_low_contrast
        ImageQualityCategory.NORMAL -> R.string.quality_normal
    }
