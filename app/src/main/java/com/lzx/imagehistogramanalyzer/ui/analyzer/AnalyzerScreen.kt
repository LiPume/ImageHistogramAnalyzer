package com.lzx.imagehistogramanalyzer.ui.analyzer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.ui.component.HistogramCard
import com.lzx.imagehistogramanalyzer.ui.component.ImagePickerCard
import com.lzx.imagehistogramanalyzer.ui.component.ImagePreviewCard
import com.lzx.imagehistogramanalyzer.ui.component.PerformanceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzerScreen(
    uiState: AnalyzerUiState,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.app_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ImagePickerCard(
                hasImage = uiState.image != null,
                isProcessing = uiState.isProcessing,
                onPickImage = onPickImage,
            )

            if (uiState.isProcessing) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.processing_image))
                }
            }

            uiState.errorMessage?.let { message ->
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

            val image = uiState.image
            val metadata = uiState.metadata
            if (image != null && metadata != null) {
                ImagePreviewCard(bitmap = image, metadata = metadata)
            }

            uiState.histogram?.let { histogram ->
                HistogramCard(histogram = histogram)
            }

            val decodeTime = uiState.decodeTimeNanos
            val calculationTime = uiState.calculationTimeNanos
            if (decodeTime != null && calculationTime != null) {
                PerformanceCard(
                    decodeTimeNanos = decodeTime,
                    calculationTimeNanos = calculationTime,
                )
            }
        }
    }
}
