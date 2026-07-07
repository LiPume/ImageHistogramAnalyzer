package com.lzx.imagehistogramanalyzer.ui.analyzer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.ui.component.HistogramCard
import com.lzx.imagehistogramanalyzer.ui.component.ImagePickerCard
import com.lzx.imagehistogramanalyzer.ui.component.ImagePreviewCard
import com.lzx.imagehistogramanalyzer.ui.component.PerformanceCard
import com.lzx.imagehistogramanalyzer.ui.component.QualityAnalysisCard
import com.lzx.imagehistogramanalyzer.ui.component.StrategySelectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzerScreen(
    uiState: AnalyzerUiState,
    onPickImage: () -> Unit,
    onSelectStrategy: (HistogramCalculationStrategy) -> Unit,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        // LazyColumn 明确承载整个结果页滚动，避免预览和直方图超出屏幕后无法查看。
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ANALYZER_LIST_TEST_TAG)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.app_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                ImagePickerCard(
                    hasImage = uiState.image != null,
                    isProcessing = uiState.isProcessing,
                    onPickImage = onPickImage,
                )
            }

            if (uiState.isProcessing) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.processing_image))
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.processing_error_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            val image = uiState.image
            val metadata = uiState.metadata
            if (image != null && metadata != null) {
                item {
                    ImagePreviewCard(bitmap = image, metadata = metadata)
                }
                item {
                    StrategySelectionCard(
                        selectedStrategy = uiState.selectedStrategy,
                        isProcessing = uiState.isProcessing,
                        onSelectStrategy = onSelectStrategy,
                        onCalculate = onCalculate,
                    )
                }
            }

            uiState.histogram?.let { histogram ->
                item {
                    HistogramCard(histogram = histogram)
                }
            }

            uiState.qualityResult?.let { qualityResult ->
                item {
                    QualityAnalysisCard(result = qualityResult)
                }
            }

            val strategy = uiState.selectedStrategy
            val decodeTime = uiState.decodeTimeNanos
            val performanceMetrics = uiState.performanceMetrics
            if (strategy != null && decodeTime != null && performanceMetrics != null) {
                item {
                    PerformanceCard(
                        strategy = strategy,
                        decodeTimeNanos = decodeTime,
                        metrics = performanceMetrics,
                    )
                }
            }
        }
    }
}

const val ANALYZER_LIST_TEST_TAG = "analyzer_list"
