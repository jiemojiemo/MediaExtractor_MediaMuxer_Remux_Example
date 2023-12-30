package com.example.mediaextractor_mediamuxer_remux_example

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class MediaMuxerTest {
    private val inputFile = "/sdcard/Android/data/com.example.mediaextractor_mediamuxer_remux_example/cache/testfile.mp4"
    private val outputFilePath = "/sdcard/Android/data/com.example.mediaextractor_mediamuxer_remux_example/cache/testoutput.mp4"
    lateinit var muxer: MediaMuxer
    lateinit var extractor: MediaExtractor
    lateinit var format: MediaFormat
    @Before
    fun setup() {
        extractor = MediaExtractor()
        muxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        extractor.setDataSource(inputFile)
        extractor.selectTrack(0)
        format = extractor.getTrackFormat(0)
    }

    @After
    fun teardown() {
        muxer.release()
        extractor.release()
    }

    @Test
    fun throwsWhenCreateWithInvalidPath() {
        val invalidPath = "/a/b/c/cache/invalid.mp4"
        assertThrows(Exception::class.java) {
            MediaMuxer(invalidPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }
    }

    @Test
    fun throwsWhenCreateWithInvalidOutputFormat() {
        assertThrows(Exception::class.java) {
            MediaMuxer(outputFilePath, 100)
        }
    }

    @Test
    fun startFailedIfNeverAddTrack() {
        assertThrows(Exception::class.java) {
            muxer.start()
        }
    }

    @Test
    fun canStartMuxer() {
        muxer.addTrack(format)
        muxer.start()

        val inputMaxSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val buffer = ByteBuffer.allocate(inputMaxSize)

        for(i in 0.. 2){
            val sampleSize = extractor.readSampleData(buffer, 0)
            val bufferInfo = MediaCodec.BufferInfo()
            bufferInfo.size = sampleSize
            bufferInfo.offset = 0
            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags
            muxer.writeSampleData(0, buffer, bufferInfo)
        }

        muxer.stop()
    }
}