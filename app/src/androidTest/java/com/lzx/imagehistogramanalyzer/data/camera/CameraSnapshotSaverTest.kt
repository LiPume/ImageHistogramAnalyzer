package com.lzx.imagehistogramanalyzer.data.camera

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class CameraSnapshotSaverTest {
    @Test
    fun save_writesBitmapToMediaStore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(80, 120, 160))
        }

        val uri = CameraSnapshotSaver(context).save(bitmap)

        try {
            context.contentResolver.openInputStream(uri).use { input ->
                assertNotNull(input)
            }
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }
}
