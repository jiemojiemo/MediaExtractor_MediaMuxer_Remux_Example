package com.example.mediaextractor_mediamuxer_remux_example

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.io.File
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    lateinit var textViewInput: TextView
    lateinit var textViewOutput: TextView
    lateinit var mediaExtractor: MediaExtractor
    lateinit var mediaMuxer: MediaMuxer
    lateinit var remuxButton: Button
    val videoTrackIndexArray = mutableListOf<Int>()
    val audioTrackIndex = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewInput = findViewById(R.id.textViewInput)
        textViewOutput = findViewById(R.id.textViewOutput)
        remuxButton = findViewById(R.id.remuxButton)

        initMediaExtractor()
        initMediaMuxer(File(externalCacheDir, "output.mp4"))

        configureMediaExtractor()
        configureMediaMuxer()

        textViewInput.text = buildFileInfoString(mediaExtractor)


        remuxButton.setOnClickListener {
            val mediaExtractor = MediaExtractor()
            try {
                resources.openRawResourceFd(R.raw.testfile).use { fd ->
                    mediaExtractor.setDataSource(fd)
                }        } catch (e: Exception) {
                e.printStackTrace()
            }
            var videoOnlyTackIndex = -1
            val outputVideoOnlyPath = File(externalCacheDir, "outputVideoOnly.mp4").absolutePath
            val mediaMuxer =
                MediaMuxer(outputVideoOnlyPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            for (trackIndex in 0 until mediaExtractor.trackCount) {
                val trackFormat = mediaExtractor.getTrackFormat(trackIndex)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                if (mime == null || !mime.startsWith("video/")) {
                    // 如果不是视频轨道，则跳过
                    continue
                }
                mediaExtractor.selectTrack(trackIndex)
                videoOnlyTackIndex = mediaMuxer.addTrack(trackFormat)
                mediaMuxer.start()
            }
            if (videoOnlyTackIndex == -1) {
                return@setOnClickListener
            }
            val bufferInfo = MediaCodec.BufferInfo()
            bufferInfo.presentationTimeUs = 0
            var sampleSize = 0
            val buffer = ByteBuffer.allocate(500 * 1024)
            val sampleTime = mediaExtractor.cachedDuration
            while (mediaExtractor.readSampleData(buffer, 0).also {
                    sampleSize = it
                } > 0) {
                bufferInfo.size = sampleSize
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                bufferInfo.presentationTimeUs = mediaExtractor.sampleTime
                mediaMuxer.writeSampleData(videoOnlyTackIndex, buffer, bufferInfo)
                mediaExtractor.advance()
            }
            mediaExtractor.release()
            mediaMuxer.stop()
            mediaMuxer.release()
            Log.d("MainActivity", "done")

        }
    }

    private fun getInputBufferFromExtractor(inputBuffer: ByteBuffer, bufferInfo: BufferInfo): Boolean{
        val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
        if(sampleSize < 0) {
            return true
        }

        val trackIndex = mediaExtractor.sampleTrackIndex
        val isAudio = audioTrackIndex.contains(trackIndex)
        val pts = mediaExtractor.sampleTime
        Log.e("MainActivity", "trackIndex: $trackIndex, pts: $pts")

        bufferInfo.size = sampleSize
        bufferInfo.presentationTimeUs = pts
        bufferInfo.offset = 0
        bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME

        return false
    }

    private fun getCurrentInputBufferTrackIndex(): Int {
        return mediaExtractor.sampleTrackIndex
    }

    private fun advanceExtractorToNextSample() {
        mediaExtractor.advance()
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaExtractor.release()

        mediaMuxer.stop()
        mediaMuxer.release()
    }

    fun initMediaExtractor() {
        try {
            mediaExtractor = MediaExtractor()
            resources.openRawResourceFd(R.raw.testfile).use { fd ->
                mediaExtractor.setDataSource(fd)
            }        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun configureMediaExtractor() {
        val trackCount = mediaExtractor.trackCount
        for (i in 0 until trackCount) {
            val format = mediaExtractor.getTrackFormat(i)
            if(format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                videoTrackIndexArray.add(i)
                mediaExtractor.selectTrack(i)
            } else if(format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex.add(i)
            }
        }
    }

    fun initMediaMuxer(outputFilePath: File){
        Log.e("MainActivity", "outputFilePath: $outputFilePath")

        try {
            mediaMuxer = MediaMuxer(outputFilePath.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun configureMediaMuxer() {
        val trackCount = mediaExtractor.trackCount
        for (i in 0 until trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            if (videoTrackIndexArray.contains(i)){
                mediaMuxer.addTrack(trackFormat)
            }
        }
    }

    fun buildFileInfoString(mediaExtractor: MediaExtractor): String {
        val stringBuilder = StringBuilder()
        val trackCount = mediaExtractor.trackCount
        stringBuilder.append("trackCount: $trackCount\n")
        stringBuilder.append("--------------------\n")
        for (i in 0 until trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            stringBuilder.append("track $i: $trackFormat\n")
            stringBuilder.append("--------------------\n")
        }
        return stringBuilder.toString()
    }
}