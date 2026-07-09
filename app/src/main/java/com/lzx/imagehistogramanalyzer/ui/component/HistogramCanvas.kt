package com.lzx.imagehistogramanalyzer.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.domain.color.ColorCastStatus
import com.lzx.imagehistogramanalyzer.domain.color.RgbChannelStats
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramNormalizer
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import kotlin.math.max

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HistogramCard(
    histogram: HistogramResult,
    rgbStats: RgbChannelStats? = null,
    modifier: Modifier = Modifier,
) {
    var selectedPage by rememberSaveable {
        mutableStateOf(HistogramChartPage.GRAYSCALE)
    }

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
            if (rgbStats != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistogramPageChip(
                        label = stringResource(R.string.grayscale_histogram_tab),
                        selected = selectedPage == HistogramChartPage.GRAYSCALE,
                        onClick = { selectedPage = HistogramChartPage.GRAYSCALE },
                    )
                    HistogramPageChip(
                        label = stringResource(R.string.rgb_histogram_tab),
                        selected = selectedPage == HistogramChartPage.RGB,
                        onClick = { selectedPage = HistogramChartPage.RGB },
                    )
                }
            }
            AnimatedContent(
                targetState = if (rgbStats == null) HistogramChartPage.GRAYSCALE else selectedPage,
                label = "histogram-chart-switch",
                transitionSpec = {
                    (
                        slideInHorizontally { width -> width / 5 } + fadeIn()
                    ).togetherWith(
                        slideOutHorizontally { width -> -width / 5 } + fadeOut()
                    )
                },
            ) { page ->
                when (page) {
                    HistogramChartPage.GRAYSCALE -> GrayscaleHistogramContent(histogram)
                    HistogramChartPage.RGB -> {
                        if (rgbStats != null) {
                            RgbHistogramContent(rgbStats)
                        } else {
                            GrayscaleHistogramContent(histogram)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistogramPageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun GrayscaleHistogramContent(histogram: HistogramResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.histogram_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HistogramCanvas(normalizedHeights = histogram.normalizedHeights)
        HistogramAxisLabels()
        Text(
            text = stringResource(R.string.histogram_max_count, histogram.maxCount),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RgbHistogramContent(rgbStats: RgbChannelStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.rgb_histogram_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RgbHistogramCanvas(rgbStats = rgbStats)
        HistogramAxisLabels()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RgbLegend(label = "R", color = Color(0xFFD84A4A))
            RgbLegend(label = "G", color = Color(0xFF3C9D69))
            RgbLegend(label = "B", color = Color(0xFF3D7FD1))
        }
        Text(
            text = stringResource(
                R.string.rgb_average_values,
                rgbStats.avgRed,
                rgbStats.avgGreen,
                rgbStats.avgBlue,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(R.string.rgb_color_cast_status, rgbStats.colorCastStatus.label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun HistogramAxisLabels() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("0", style = MaterialTheme.typography.labelSmall)
        Text("128", style = MaterialTheme.typography.labelSmall)
        Text("255", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RgbLegend(label: String, color: Color) {
    Row {
        Canvas(
            modifier = Modifier
                .padding(top = 7.dp)
                .height(8.dp)
                .width(18.dp),
        ) {
            drawLine(
                color = color,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 4.dp.toPx(),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
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

@Composable
fun RgbHistogramCanvas(
    rgbStats: RgbChannelStats,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        R.string.rgb_histogram_accessibility_description,
        rgbStats.avgRed,
        rgbStats.avgGreen,
        rgbStats.avgBlue,
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HISTOGRAM_ASPECT_RATIO)
            .background(Color.White)
            .semantics { contentDescription = description },
    ) {
        val maxCount = maxOf(
            rgbStats.redCounts.maxOrNull() ?: 0,
            rgbStats.greenCounts.maxOrNull() ?: 0,
            rgbStats.blueCounts.maxOrNull() ?: 0,
        ).coerceAtLeast(1)
        drawChannelPath(rgbStats.redCounts, maxCount, Color(0xFFD84A4A))
        drawChannelPath(rgbStats.greenCounts, maxCount, Color(0xFF3C9D69))
        drawChannelPath(rgbStats.blueCounts, maxCount, Color(0xFF3D7FD1))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChannelPath(
    counts: IntArray,
    maxCount: Int,
    color: Color,
) {
    val stepX = size.width / (HistogramResult.GRAY_LEVELS - 1)
    val path = Path()
    counts.forEachIndexed { index, count ->
        val safeCount = count.coerceAtLeast(0)
        val y = size.height - size.height * safeCount / maxCount
        val x = index * stepX
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx()),
    )
}

private enum class HistogramChartPage {
    GRAYSCALE,
    RGB,
}

private val ColorCastStatus.label: String
    get() = when (this) {
        ColorCastStatus.BALANCED -> "色彩较均衡"
        ColorCastStatus.SLIGHT_RED -> "轻微偏红"
        ColorCastStatus.SLIGHT_GREEN -> "轻微偏绿"
        ColorCastStatus.SLIGHT_BLUE -> "轻微偏蓝"
        ColorCastStatus.RED_CAST -> "偏红或偏暖"
        ColorCastStatus.GREEN_CAST -> "偏绿"
        ColorCastStatus.BLUE_CAST -> "偏蓝或偏冷"
    }

private const val HISTOGRAM_ASPECT_RATIO = 256f / 100f
