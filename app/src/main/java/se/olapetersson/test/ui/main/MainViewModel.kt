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
    // Might be able to use to analyze sound data: https://newventuresoftware.com/blog/record-play-and-visualize-raw-audio-data-in-android.html
    // https://github.com/newventuresoftware/WaveformControl/blob/master/app/src/main/java/com/newventuresoftware/waveformdemo/MainActivity.java
    init {
        debug("initting in viewmodel")
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource("/sdcard/Download/test.mp4")
        val trackCount = mediaExtractor.trackCount
        debug("This is the trackCount: $trackCount")
        val muxer = MediaMuxer("/sdcard/Download/output.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        //(0 until mediaExtractor.trackCount).forEach {
            val format = mediaExtractor.getTrackFormat(1)
            debug("this is the format $format")
            // index 1 seems to be the audio
            val audioTrack = 1
            mediaExtractor.selectTrack(audioTrack)
            muxer.addTrack(format)
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
                    debug("Saw input EOS.")
                    debug("this is the audioByteBuffer $audioByteBuffer")
                    audioByteBuffer.rewind()
                    debug("audi bytebuffer has remaingin: ${audioByteBuffer.hasRemaining()}")
                    while(audioByteBuffer.hasRemaining()) {
                        debug("bajtbuffer: ${audioByteBuffer.position()} : ${audioByteBuffer.get()}")
                    }
                    bufferInfo.size = 0;
                    break;
                } else {
                    bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime()
                    if (endMs > 0 && bufferInfo.presentationTimeUs > (endMs * 1000)) {
                        debug("The current sample is over the trim end time.")
                        break;
                    } else {
                        bufferInfo.flags = mediaExtractor.sampleFlags
                        val trackIndex = mediaExtractor.sampleTrackIndex
                        muxer.writeSampleData(0, audioByteBuffer, bufferInfo)
                        mediaExtractor.advance()
                    }
                }
            }
            muxer.stop()
            muxer.release()
        //}
    }
}


inline fun debug(string: String) {
    Log.i("DEBUG", string)
}