package com.example.gympassion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import android.view.Gravity
import android.view.View

class VideoViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_view)

        val exerciseName = intent.getStringExtra("exerciseName") ?: "Default Exercise"
        title = exerciseName

        val videoUrl = intent.getStringExtra("videoUrl")
        val videoView = findViewById<VideoView>(R.id.videoView)

        val mediaController = object : MediaController(this) {
            override fun setAnchorView(view: View) {
                super.setAnchorView(view)

                val fullScreenButton = ImageButton(this@VideoViewActivity)
                fullScreenButton.setImageResource(R.drawable.ic_media_fullscreen)
                fullScreenButton.setOnClickListener {
                    val intent = Intent(this@VideoViewActivity, FullScreenVideoActivity::class.java)
                    intent.putExtra("videoUrl", videoUrl)
                    intent.putExtra("exerciseName", exerciseName) // przekazujemy nazwę ćwiczenia
                    startActivity(intent)
                }

                val closeButton = ImageButton(this@VideoViewActivity)
                closeButton.setImageResource(R.drawable.ic_close)
                closeButton.setOnClickListener {
                    finish()
                }

                val frameParamsFullScreen = FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.RIGHT or Gravity.BOTTOM
                )

                addView(fullScreenButton, frameParamsFullScreen)

                val frameParamsClose = FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.RIGHT or Gravity.TOP
                )

                addView(closeButton, frameParamsClose)

            }
        }

        videoView.setMediaController(mediaController)
        videoView.setVideoURI(Uri.parse(videoUrl))
        videoView.requestFocus()

        // Start playing the video once it's prepared
        videoView.setOnPreparedListener {
            videoView.start()
        }

        // Restart the video when it's finished playing
        videoView.setOnCompletionListener {
            videoView.start()
        }
    }
}
