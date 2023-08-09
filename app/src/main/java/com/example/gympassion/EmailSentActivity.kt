package com.example.gympassion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class EmailSentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_sent)
        val animationView = findViewById<LottieAnimationView>(R.id.animationView)

        // Sprawdzanie, czy plik animacji został załadowany poprawnie
        Log.d("EmailSentActivity", "Animation file name: ${animationView.animation}")

        animationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                // Logowanie rozpoczęcia animacji
                Log.d("EmailSentActivity", "Animation started")
            }

            override fun onAnimationEnd(animation: Animator) {
                // Logowanie zakończenia animacji
                Log.d("EmailSentActivity", "Animation ended")
                val intent = Intent(this@EmailSentActivity, MainActivity::class.java)
                startActivity(intent)
            }

            override fun onAnimationCancel(animation: Animator) {
                // Logowanie anulowania animacji
                Log.d("EmailSentActivity", "Animation cancelled")
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Logowanie powtórzenia animacji
                Log.d("EmailSentActivity", "Animation repeated")
            }
        })
    }
}

