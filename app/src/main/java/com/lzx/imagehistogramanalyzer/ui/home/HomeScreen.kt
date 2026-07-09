package com.lzx.imagehistogramanalyzer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.ui.theme.AppSpacing
import com.lzx.imagehistogramanalyzer.ui.theme.ImageHistogramAnalyzerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasSelectedImage: Boolean,
    onPickImage: () -> Unit,
    onResumeAnalysis: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = AppSpacing.medium,
                vertical = AppSpacing.small,
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            item {
                HeroCard(
                    hasSelectedImage = hasSelectedImage,
                    onPickImage = onPickImage,
                    onResumeAnalysis = onResumeAnalysis,
                    onOpenCamera = onOpenCamera,
                )
            }
            item {
                SectionTitle(stringResource(R.string.home_capabilities_title))
            }
            item {
                FeatureCard(
                    number = "01",
                    title = stringResource(R.string.home_histogram_feature_title),
                    description = stringResource(R.string.home_histogram_feature_description),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            item {
                FeatureCard(
                    number = "02",
                    title = stringResource(R.string.home_quality_feature_title),
                    description = stringResource(R.string.home_quality_feature_description),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            item {
                FeatureCard(
                    number = "03",
                    title = stringResource(R.string.home_performance_feature_title),
                    description = stringResource(R.string.home_performance_feature_description),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            item {
                FeatureCard(
                    number = "04",
                    title = stringResource(R.string.home_camera_feature_title),
                    description = stringResource(R.string.home_camera_feature_description),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            item {
                SectionTitle(stringResource(R.string.home_steps_title))
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    ) {
                        StepRow("1", stringResource(R.string.home_step_select))
                        StepRow("2", stringResource(R.string.home_step_strategy))
                        StepRow("3", stringResource(R.string.home_step_result))
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
                    ) {
                        Text(
                            text = stringResource(R.string.home_target_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.home_target_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    hasSelectedImage: Boolean,
    onPickImage: () -> Unit,
    onResumeAnalysis: () -> Unit,
    onOpenCamera: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        ) {
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.home_hero_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.home_local_privacy),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(
                onClick = onPickImage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.select_image))
            }
            if (hasSelectedImage) {
                OutlinedButton(
                    onClick = onResumeAnalysis,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.resume_analysis))
                }
            }
            OutlinedButton(
                onClick = onOpenCamera,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.open_realtime_camera))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun FeatureCard(
    number: String,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(AppSpacing.extraLarge)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AppSpacing.large)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(name = "首页 Light", widthDp = 360, showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ImageHistogramAnalyzerTheme {
        HomeScreen(
            hasSelectedImage = true,
            onPickImage = {},
            onResumeAnalysis = {},
            onOpenCamera = {},
        )
    }
}
