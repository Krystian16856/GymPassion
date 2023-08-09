package com.example.gympassion

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class FullScreenVideoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_video)

        // Make this activity full screen
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val videoUrl = intent.getStringExtra("videoUrl")
        val exerciseName = intent.getStringExtra("exerciseName")
        title = exerciseName

        val videoView = findViewById<VideoView>(R.id.videoView)


        val mediaController = object : MediaController(this) {
            override fun setAnchorView(view: View) {
                super.setAnchorView(view)

                val closeButton = ImageButton(this@FullScreenVideoActivity)
                closeButton.setImageResource(R.drawable.ic_media_close_fullscreen)
                closeButton.setOnClickListener {
                    finish() // Finish FullScreenVideoActivity
                }

                val frameParams = FrameLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    Gravity.RIGHT or Gravity.TOP
                )

                addView(closeButton, frameParams)
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
