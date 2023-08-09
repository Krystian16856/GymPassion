package com.example.gympassion

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage

class TextViewActivity : AppCompatActivity() {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_view)

        val textUrl = intent.getStringExtra("textUrl")
        Log.d("TextViewActivity", "Received textUrl: $textUrl")

        val textView = findViewById<TextView>(R.id.textView)
        val closeButton = findViewById<ImageView>(R.id.closeButton)

        closeButton.setOnClickListener {
            finish()
        }

        // Fetch the text content from the Firebase Storage
        val storageReference = storage.reference.child(textUrl!!)
        storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val description = String(bytes)
            textView.text = description
        }.addOnFailureListener { exception ->
            Log.d("TextViewActivity", "Failed to download text: $exception")
        }
    }
}
