package com.example.mediaextractor_mediamuxer_remux_example

import android.content.res.AssetFileDescriptor
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
    lateinit var remuxButton: Button
    lateinit var extractVideoButton: Button
    lateinit var extractAudioButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewInput = findViewById(R.id.textViewInput)
        textViewOutput = findViewById(R.id.textViewOutput)
        remuxButton = findViewById(R.id.remuxButton)
        extractVideoButton = findViewById(R.id.extractVideoButton)
        extractAudioButton = findViewById(R.id.extractAudioButton)

        val remuxOutputFileName = "remux_output.mp4"
        val remuxOutputFilePath = File(externalCacheDir, remuxOutputFileName).absolutePath
        remuxButton.setOnClickListener {
            remux(remuxOutputFilePath)
        }

        val extractVideoOutputName = "extract_video_output.mp4"
        val extractVideoOutputPath = File(externalCacheDir, extractVideoOutputName).absolutePath
        extractVideoButton.setOnClickListener {
            extractVideo(extractVideoOutputPath)
        }

        val extractAudioOutputName = "extract_audio_output.mp4"
        val extractAudioOutputPath = File(externalCacheDir, extractAudioOutputName).absolutePath
        extractAudioButton.setOnClickListener {
            extractAudio(extractAudioOutputPath)
        }

    }


    private fun remux(outputFilePath: String) {
        val mediaExtractor = MediaExtractor()
        try {
            resources.openRawResourceFd(R.raw.testfile).use { fd ->
                mediaExtractor.setDataSource(fd)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        textViewInput.text = buildFileInfo(mediaExtractor)

        val mediaMuxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackCount = mediaExtractor.trackCount
        var maxInputSize = 0
        for (i in 0 until trackCount){
            val trackFormat = mediaExtractor.getTrackFormat(i)
            val maxInputSizeFromThisTrack = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            if (maxInputSizeFromThisTrack > maxInputSize) {
                maxInputSize = maxInputSizeFromThisTrack
            }

            mediaExtractor.selectTrack(i)
            mediaMuxer.addTrack(trackFormat)
        }

        val inputBuffer = ByteBuffer.allocate(maxInputSize)
        val bufferInfo = BufferInfo()
        mediaMuxer.start()
        while(true)
        {
            val isInputBufferEnd = getInputBufferFromExtractor(mediaExtractor, inputBuffer, bufferInfo)
            if (isInputBufferEnd) {
                break
            }
            mediaMuxer.writeSampleData(mediaExtractor.sampleTrackIndex, inputBuffer, bufferInfo)
            mediaExtractor.advance()
        }

        mediaMuxer.stop()
        mediaMuxer.release()
        mediaExtractor.release()

        textViewOutput.text = buildFileInfo(outputFilePath)
    }

    private fun extractVideo(outputFilePath: String){
        val mediaExtractor = MediaExtractor()
        try {
            resources.openRawResourceFd(R.raw.testfile).use { fd ->
                mediaExtractor.setDataSource(fd)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        textViewInput.text = buildFileInfo(mediaExtractor)

        val mediaMuxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackCount = mediaExtractor.trackCount
        var maxInputSize = 0
        for (i in 0 until trackCount){
            val trackFormat = mediaExtractor.getTrackFormat(i)
            if(isVideoTrack(trackFormat)){
                val maxInputSizeFromThisTrack = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                if (maxInputSizeFromThisTrack > maxInputSize) {
                    maxInputSize = maxInputSizeFromThisTrack
                }
                mediaExtractor.selectTrack(i)
                mediaMuxer.addTrack(trackFormat)
            }
        }

        val inputBuffer = ByteBuffer.allocate(maxInputSize)
        val bufferInfo = BufferInfo()
        mediaMuxer.start()
        while(true)
        {
            val isInputBufferEnd = getInputBufferFromExtractor(mediaExtractor, inputBuffer, bufferInfo)
            if (isInputBufferEnd) {
                break
            }
            mediaMuxer.writeSampleData(0, inputBuffer, bufferInfo)
            mediaExtractor.advance()
        }

        mediaMuxer.stop()
        mediaMuxer.release()
        mediaExtractor.release()

        textViewOutput.text = buildFileInfo(outputFilePath)
    }

    private fun extractAudio(outputFilePath: String){
        val mediaExtractor = MediaExtractor()
        try {
            resources.openRawResourceFd(R.raw.testfile).use { fd ->
                mediaExtractor.setDataSource(fd)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        textViewInput.text = buildFileInfo(mediaExtractor)

        val mediaMuxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val trackCount = mediaExtractor.trackCount
        var maxInputSize = 0
        for (i in 0 until trackCount){
            val trackFormat = mediaExtractor.getTrackFormat(i)
            if(isAudioTrack(trackFormat)){
                val maxInputSizeFromThisTrack = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                if (maxInputSizeFromThisTrack > maxInputSize) {
                    maxInputSize = maxInputSizeFromThisTrack
                }
                mediaExtractor.selectTrack(i)
                mediaMuxer.addTrack(trackFormat)
            }
        }

        val inputBuffer = ByteBuffer.allocate(maxInputSize)
        val bufferInfo = BufferInfo()
        mediaMuxer.start()
        while(true)
        {
            val isInputBufferEnd = getInputBufferFromExtractor(mediaExtractor, inputBuffer, bufferInfo)
            if (isInputBufferEnd) {
                break
            }
            mediaMuxer.writeSampleData(0, inputBuffer, bufferInfo)
            mediaExtractor.advance()
        }

        mediaMuxer.stop()
        mediaMuxer.release()
        mediaExtractor.release()

        textViewOutput.text = buildFileInfo(outputFilePath)
    }

    private fun isVideoTrack(mediaFormat: MediaFormat): Boolean{
        val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
        return mime?.startsWith("video/") ?: false
    }

    private fun isAudioTrack(mediaFormat: MediaFormat): Boolean{
        val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
        return mime?.startsWith("audio/") ?: false
    }

    private fun getInputBufferFromExtractor(
        mediaExtractor: MediaExtractor,
        inputBuffer: ByteBuffer,
        bufferInfo: BufferInfo
    ): Boolean {
        val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
        if (sampleSize < 0) {
            return true
        }

        val trackIndex = mediaExtractor.sampleTrackIndex
        val pts = mediaExtractor.sampleTime
        Log.e("MainActivity", "trackIndex: $trackIndex, pts: $pts")

        bufferInfo.size = sampleSize
        bufferInfo.presentationTimeUs = pts
        bufferInfo.offset = 0
        bufferInfo.flags = mediaExtractor.sampleFlags

        return false
    }

    private fun buildFileInfo(filePath: String): String {
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(filePath)
        return buildFileInfo(mediaExtractor)
    }

    private fun buildFileInfo(fd: AssetFileDescriptor): String{
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(fd)
        return buildFileInfo(mediaExtractor)
    }

    private fun buildFileInfo(extractor: MediaExtractor): String{
        val stringBuilder = StringBuilder()
        val trackCount = extractor.trackCount
        stringBuilder.append("trackCount: $trackCount\n")
        stringBuilder.append("--------------------\n")
        for (i in 0 until trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            stringBuilder.append("track $i: $trackFormat\n")
            stringBuilder.append("--------------------\n")
        }
        return stringBuilder.toString()
    }
}