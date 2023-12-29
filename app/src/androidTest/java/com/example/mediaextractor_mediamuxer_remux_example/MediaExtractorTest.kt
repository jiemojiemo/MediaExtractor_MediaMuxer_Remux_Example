package com.example.mediaextractor_mediamuxer_remux_example

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.runner.RunWith

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class MediaExtractorTest {
    private val e = MediaExtractor()
    private val testFile =
        "/sdcard/Android/data/com.example.mediaextractor_mediamuxer_remux_example/cache/testfile.mp4"

    @After
    fun teardown() {
        e.release()
    }

    @Test
    fun canCreateAndRelease() {
        val extractor = MediaExtractor()
        extractor.release()
    }

    @Test
    fun trackCountIsZeroIfNeverSetSource() {
        val trackCount = e.trackCount

        assertEquals(0, trackCount)
    }

    @Test
    fun setDataSourceWithStringPath() {
        val testFile =
            "/sdcard/Android/data/com.example.mediaextractor_mediamuxer_remux_example/cache/testfile.mp4"
        e.setDataSource(testFile)
    }

    @Test
    fun throwsIfSetDataSourceWithInvalidPath() {
        val invalidPath =
            "/sdcard/Android/data/com.example.mediaextractor_mediamuxer_remux_example/cache/invalid.mp4"
        assertThrows(Exception::class.java) {
            e.setDataSource(invalidPath)
        }
    }

    @Test
    fun canGetSourceTrackCount() {
        e.setDataSource(testFile)

        val trackCount = e.trackCount

        assertEquals(2, trackCount)
    }

    @Test
    fun canGetTrackFormatByIndex() {
        e.setDataSource(testFile)

        for (i in 0 until e.trackCount) {
            val trackFormat = e.getTrackFormat(i)

            assertNotNull(trackFormat)

            Log.i("MediaExtractorTest", "Track format for track $i: $trackFormat")
        }
    }

    @Test
    fun canGetMimeFromTrackFormat() {
        e.setDataSource(testFile)

        val videoTrackFormat = e.getTrackFormat(0)
        val audioTrackFormat = e.getTrackFormat(1)

        val videoMime = videoTrackFormat.getString(MediaFormat.KEY_MIME)
        val audioMimeType = audioTrackFormat.getString(MediaFormat.KEY_MIME)

        assertTrue(videoMime!!.startsWith("video/"))
        assertTrue(audioMimeType!!.startsWith("audio/"))
    }

    @Test
    fun canGetMaxInputSizeFromTrackFormat() {
        e.setDataSource(testFile)

        val videoTrackFormat = e.getTrackFormat(0)
        val audioTrackFormat = e.getTrackFormat(1)

        val videoMaxInputSize = videoTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val audioMaxInputSize = audioTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)

        assertTrue(videoMaxInputSize > 0)
        assertTrue(audioMaxInputSize > 0)
    }

    @Test
    fun throwsIllegalArgumentExceptionIfGetTrackFormatWithInvalidIndex() {
        e.setDataSource(testFile)

        assertThrows(IllegalArgumentException::class.java) {
            e.getTrackFormat(10)
        }
    }

    @Test
    fun readFailedIfNeverSetDataSource() {
        val buffer = ByteBuffer.allocate(1024)
        val sampleSize = e.readSampleData(buffer, 0)

        assertEquals(-1, sampleSize)
    }

    @Test
    fun readFailedIfNeverSelectTrack() {
        e.setDataSource(testFile)

        val buffer = ByteBuffer.allocate(1024)
        val sampleSize = e.readSampleData(buffer, 0)

        assertEquals(-1, sampleSize)
    }

    @Test
    fun canSelectAndUnselectTrack() {
        e.setDataSource(testFile)

        e.selectTrack(0)
        e.unselectTrack(0)
    }

    @Test
    fun selectFailedIfInvalidTrackIndex() {
        e.setDataSource(testFile)

        assertThrows(IllegalArgumentException::class.java) {
            e.selectTrack(10)
        }
    }

    @Test
    fun unselectFailedIfInvalidTrackIndex() {
        e.setDataSource(testFile)

        assertThrows(IllegalArgumentException::class.java) {
            e.unselectTrack(10)
        }
    }

    @Test
    fun canReadSamples() {
        e.setDataSource(testFile)
        val trackIndex = 0
        val format = e.getTrackFormat(trackIndex)
        val maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val buffer = ByteBuffer.allocate(maxInputSize)

        e.selectTrack(trackIndex)
        val sampleSize = e.readSampleData(buffer, 0)

        assertTrue(sampleSize > 0)
    }

    @Test
    fun readSampleFailedIfBufferTooSmall() {
        e.setDataSource(testFile)
        val trackIndex = 0
        val buffer = ByteBuffer.allocate(1)

        e.selectTrack(trackIndex)

        assertThrows(IllegalArgumentException::class.java) {
            e.readSampleData(buffer, 0)
        }
    }

    @Test
    fun canGetSampleInfoAfterRead(){
        e.setDataSource(testFile)
        val trackIndex = 0
        val format = e.getTrackFormat(trackIndex)
        val maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val buffer = ByteBuffer.allocate(maxInputSize)

        e.selectTrack(trackIndex)
        e.readSampleData(buffer, 0)

        assertTrue(e.sampleTime >= 0)
        assertEquals(e.sampleTrackIndex, trackIndex)
    }

    @Test
    fun readSameSampleIfNeverAdvance() {
        e.setDataSource(testFile)
        val trackIndex = 0
        val format = e.getTrackFormat(trackIndex)
        val maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val buffer = ByteBuffer.allocate(maxInputSize)

        e.selectTrack(trackIndex)
        val sampleSize = e.readSampleData(buffer, 0)
        val sampleTime = e.sampleTime
        val sampleFlags = e.sampleFlags


        val sampleSize2 = e.readSampleData(buffer, 0)
        val sampleTime2 = e.sampleTime
        val sampleFlags2 = e.sampleFlags

        assertEquals(sampleSize, sampleSize2)
        assertEquals(sampleTime, sampleTime2)
        assertEquals(sampleFlags, sampleFlags2)
    }

    @Test
    fun readNextSampleAfterAdvance(){
        e.setDataSource(testFile)
        val trackIndex = 0
        val format = e.getTrackFormat(trackIndex)
        val maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val buffer = ByteBuffer.allocate(maxInputSize)

        e.selectTrack(trackIndex)
        val sampleSize = e.readSampleData(buffer, 0)
        val sampleTime = e.sampleTime

        e.advance()

        val sampleSize2 = e.readSampleData(buffer, 0)
        val sampleTime2 = e.sampleTime

        assertTrue(sampleTime2 > sampleTime)
    }
}