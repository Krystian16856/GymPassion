package com.example.gympassion

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class ImageViewActivity : AppCompatActivity() {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        val imageUrl = intent.getStringExtra("imageUrl")
        Log.d("ImageViewActivity", "Received imageUrl: $imageUrl")
        val imageType = intent.getStringExtra("imageType")
        Log.d("ImageViewActivity", "Received imageType: $imageType")

        val imageView = findViewById<ImageView>(R.id.imageView)
        val closeButton = findViewById<ImageView>(R.id.closeButton)

        closeButton.setOnClickListener {
            finish()
        }

        if (imageType == "png") {
            Glide.with(this)
                .load(imageUrl)
                .into(imageView)
        } else if (imageType == "webp" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val file = File(imageUrl)

            Thread {
                val source = ImageDecoder.createSource(file)
                val drawable = ImageDecoder.decodeDrawable(source)
                if (drawable is AnimatedImageDrawable) {
                    drawable.start()
                }
                runOnUiThread {
                    imageView.setImageDrawable(drawable)
                }
            }.start()
        }
    }
}