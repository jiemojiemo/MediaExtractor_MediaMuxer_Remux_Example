package com.example.mediaextractor_mediamuxer_remux_example

import android.content.res.AssetManager
import android.media.MediaExtractor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)

        // create media extractor
        val mediaExtractor = MediaExtractor()
        resources.openRawResourceFd(R.raw.testfile).use { fd ->
            mediaExtractor.setDataSource(fd)
        }

        textView.text = buildFileInfoString(mediaExtractor)
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