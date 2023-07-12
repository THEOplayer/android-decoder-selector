package com.theoplayer.android.decoderselector

import android.os.Build
import com.theoplayer.android.api.settings.DecoderType
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(RobolectricTestRunner::class)
class ExternalDecoderSelectionHelperTest {

    private val decoderSelectionHelper = ExternalDecoderSelectionHelper()
    @Test
    fun xiaomiM1_isDecoderSelectionFixed() {
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "tissot")
        assertFalse(decoderSelectionHelper.shouldUseDecoder(DecoderType.VIDEO, "OMX.qcom.video.decoder.avc", null))
    }
}