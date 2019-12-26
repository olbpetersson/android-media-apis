package se.olapetersson.test.ui.main

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import androidx.lifecycle.ViewModel
import java.nio.ByteBuffer


class MainViewModel : ViewModel() {

    // most of this is from https://stackoverflow.com/questions/35379000/extract-audio-from-mp4-and-save-to-sd-card-mediaextractor
    init {
        "initting in viewmodel".debug()
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource("/sdcard/Download/test.mp4")
        val trackCount = mediaExtractor.trackCount
        "This is the trackCount: $trackCount".debug()
        val muxer = MediaMuxer("/sdcard/Download/output", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        (0 until mediaExtractor.trackCount).forEach {
            "$it".debug()
            val format = mediaExtractor.getTrackFormat(it)
            "this is the format $format".debug()
            // index 1 seems to be the audio
            val audioTrack = mediaExtractor.selectTrack(1)
            "this is the audioTrack $audioTrack".debug()
            var bufferSize = 0
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                bufferSize = if (newSize > bufferSize) newSize else bufferSize
            }
            val audioByteBuffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            muxer.start()
            val endMs = -1
            while (true) {
                bufferInfo.offset = 0;
                bufferInfo.size = mediaExtractor.readSampleData(audioByteBuffer, 0)
                if (bufferInfo.size < 0) {
                    "Saw input EOS.".debug()
                    bufferInfo.size = 0;
                    break;
                } else {
                    bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime()
                    if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
                        "The current sample is over the trim end time.".debug()
                        break;
                    } else {
                        bufferInfo.flags = mediaExtractor.getSampleFlags()
                        val trackIndex = mediaExtractor.getSampleTrackIndex()
                        muxer.writeSampleData(1, audioByteBuffer, bufferInfo)
                        mediaExtractor.advance()
                    }
                }
            }
            muxer.stop()
            muxer.release()
        }
    }
}


fun String.debug() {
    Log.i("DEBUG", this)
}