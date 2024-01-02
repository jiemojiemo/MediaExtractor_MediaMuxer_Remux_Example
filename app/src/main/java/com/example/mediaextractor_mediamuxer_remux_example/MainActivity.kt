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
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    lateinit var textViewInput: TextView
    lateinit var textViewOutput: TextView
    lateinit var mixingButton: Button
    lateinit var extractVideoButton: Button
    lateinit var extractAudioButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewInput = findViewById(R.id.textViewInput)
        textViewOutput = findViewById(R.id.textViewOutput)
        mixingButton = findViewById(R.id.mixingButton)
        extractVideoButton = findViewById(R.id.extractVideoButton)
        extractAudioButton = findViewById(R.id.extractAudioButton)

        val mixingOutputFileName = "mixing_output.mp4"
        val mixingOutputFilePath = File(externalCacheDir, mixingOutputFileName).absolutePath
        mixingButton.setOnClickListener {
            mixAudioAndVideo(mixingOutputFilePath)
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
                break
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

    private fun mixAudioAndVideo(outputFilePath: String){
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()

        try {
            resources.openRawResourceFd(R.raw.testfile).use { fd ->
                videoExtractor.setDataSource(fd)
            }

            resources.openRawResourceFd(R.raw.music).use { fd ->
                audioExtractor.setDataSource(fd)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        textViewInput.text = buildFileInfo(videoExtractor)

        val mediaMuxer = MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        videoExtractor.selectTrack(0)
        val videoTrackFormat = videoExtractor.getTrackFormat(0)
        val muxerVideoTrackIndex = mediaMuxer.addTrack(videoTrackFormat)

        // audio track at 1 in this file
        audioExtractor.selectTrack(1)
        val audioTrackFormat = audioExtractor.getTrackFormat(1)
        val muxerAudioTrackIndex = mediaMuxer.addTrack(audioTrackFormat)

        val videoTrackDuration = videoTrackFormat.getLong(MediaFormat.KEY_DURATION)
        val videoMaxInputSize = videoTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val audioTrackDuration = audioTrackFormat.getLong(MediaFormat.KEY_DURATION)
        val audioMaxInputSize = audioTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val targetDuration = min(videoTrackDuration, audioTrackDuration)
        val targetMaxInputSize = max(videoMaxInputSize, audioMaxInputSize)
        val inputVideoBuffer = ByteBuffer.allocate(targetMaxInputSize)
        val bufferInfo = BufferInfo()

        mediaMuxer.start()
        while(true){
            val isVideoInputEnd = getInputBufferFromExtractor(videoExtractor, inputVideoBuffer, bufferInfo)
            if(isVideoInputEnd || bufferInfo.presentationTimeUs >= targetDuration){
                break
            }
            mediaMuxer.writeSampleData(muxerVideoTrackIndex, inputVideoBuffer, bufferInfo)
            videoExtractor.advance()
        }

        while(true){
            val isAudioInputEnd = getInputBufferFromExtractor(audioExtractor, inputVideoBuffer, bufferInfo)
            if(isAudioInputEnd || bufferInfo.presentationTimeUs >= targetDuration){
                break
            }
            mediaMuxer.writeSampleData(muxerAudioTrackIndex, inputVideoBuffer, bufferInfo)
            audioExtractor.advance()
        }

        mediaMuxer.stop()
        mediaMuxer.release()
        videoExtractor.release()
        audioExtractor.release()
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

        bufferInfo.size = sampleSize
        bufferInfo.presentationTimeUs = mediaExtractor.sampleTime
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