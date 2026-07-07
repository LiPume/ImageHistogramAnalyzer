package com.lzx.imagehistogramanalyzer.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramNormalizer
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import kotlin.math.max

@Composable
fun HistogramCard(
    histogram: HistogramResult,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.histogram_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.histogram_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HistogramCanvas(normalizedHeights = histogram.normalizedHeights)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0", style = MaterialTheme.typography.labelSmall)
                Text("128", style = MaterialTheme.typography.labelSmall)
                Text("255", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = stringResource(R.string.histogram_max_count, histogram.maxCount),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun HistogramCanvas(
    normalizedHeights: IntArray,
    modifier: Modifier = Modifier,
) {
    require(normalizedHeights.size == HistogramResult.GRAY_LEVELS) {
        "直方图绘制数据必须包含 256 项"
    }
    val description = stringResource(R.string.histogram_accessibility_description)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HISTOGRAM_ASPECT_RATIO)
            .background(Color.White)
            .semantics { contentDescription = description },
    ) {
        val barWidth = size.width / HistogramResult.GRAY_LEVELS
        normalizedHeights.forEachIndexed { index, normalizedHeight ->
            val safeHeight = normalizedHeight.coerceIn(0, HistogramNormalizer.NORMALIZED_MAX)
            val pixelHeight = size.height * safeHeight / HistogramNormalizer.NORMALIZED_MAX
            drawRect(
                color = Color.Black,
                topLeft = Offset(index * barWidth, size.height - pixelHeight),
                size = Size(max(1f, barWidth), pixelHeight),
            )
        }
    }
}

private const val HISTOGRAM_ASPECT_RATIO = 256f / 100f
