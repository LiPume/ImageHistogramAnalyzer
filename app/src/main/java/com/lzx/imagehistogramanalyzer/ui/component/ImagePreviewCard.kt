package com.lzx.imagehistogramanalyzer.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.R
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import java.text.NumberFormat

@Composable
fun ImagePreviewCard(
    bitmap: Bitmap,
    metadata: ImageMetadata,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val pixelCount = remember(metadata.pixelCount) {
        NumberFormat.getIntegerInstance().format(metadata.pixelCount)
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.image_preview_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            androidx.compose.foundation.Image(
                bitmap = imageBitmap,
                contentDescription = stringResource(R.string.selected_image_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Fit,
            )
            Text(metadata.displayName, style = MaterialTheme.typography.bodyLarge)
            MetadataRow(
                label = stringResource(R.string.image_resolution),
                value = "${metadata.width} × ${metadata.height}",
            )
            MetadataRow(
                label = stringResource(R.string.image_pixel_count),
                value = pixelCount,
            )
            MetadataRow(
                label = stringResource(R.string.image_mime_type),
                value = metadata.mimeType,
            )
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
