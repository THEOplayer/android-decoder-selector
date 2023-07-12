package com.theoplayer.android.decoderselector

import android.media.MediaCodecInfo
import com.theoplayer.android.api.settings.DecoderSelectionHelper
import com.theoplayer.android.api.settings.DecoderType

class ExternalDecoderSelectionHelper : DecoderSelectionHelper() {
    override fun shouldUseDecoder(
        decoderType: DecoderType?,
        decoderName: String?,
        codecInfo: MediaCodecInfo?
    ): Boolean {
        return super.shouldUseDecoder(decoderType, decoderName, codecInfo)
    }
}