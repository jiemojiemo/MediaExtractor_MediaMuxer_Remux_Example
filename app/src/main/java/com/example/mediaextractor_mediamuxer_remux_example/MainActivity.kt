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
        val files = assets.list("")
        Log.e("MainActivity", "files: ${files?.joinToString(", ")}")

//        // create media extractor
//        val mediaExtractor = MediaExtractor()
//        assets.openFd("clips/testfile.mp4").use { assetFileDescriptor ->
//            mediaExtractor.setDataSource(
//                assetFileDescriptor.fileDescriptor,
//            )
//        }
//
//        // get track count
//        val trackCount = mediaExtractor.trackCount
//        textView.text = "trackCount: $trackCount"
    }
}