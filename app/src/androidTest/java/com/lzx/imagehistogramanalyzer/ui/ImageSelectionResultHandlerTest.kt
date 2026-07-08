package com.lzx.imagehistogramanalyzer.ui

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImageSelectionResultHandlerTest {
    @Test
    fun canceledPicker_keepsCurrentPageAndDoesNotSelectImage() {
        var selectedUri: Uri? = null
        var analyzerOpened = false

        handlePhotoPickerResult(
            uri = null,
            onImageSelected = { selectedUri = it },
            onOpenAnalyzer = { analyzerOpened = true },
        )

        assertEquals(null, selectedUri)
        assertFalse(analyzerOpened)
    }
}
