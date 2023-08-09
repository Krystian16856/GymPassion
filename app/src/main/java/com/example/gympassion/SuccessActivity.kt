package com.example.gympassion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.airbnb.lottie.LottieAnimationView

class SuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)
        val animationView = findViewById<LottieAnimationView>(R.id.animationView)
        title = "Success"


        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        // Sprawdzanie, czy plik animacji został załadowany poprawnie
        Log.d("SuccessActivity", "Animation file name: ${animationView.animation}")

        animationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                // Logowanie rozpoczęcia animacji
                Log.d("SuccessActivity", "Animation started")
            }

            override fun onAnimationEnd(animation: Animator) {
                // Logowanie zakończenia animacji
                Log.d("SuccessActivity", "Animation ended")
                val intent = Intent(this@SuccessActivity, MainActivity::class.java)
                startActivity(intent)
            }

            override fun onAnimationCancel(animation: Animator) {
                // Logowanie anulowania animacji
                Log.d("SuccessActivity", "Animation cancelled")
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Logowanie powtórzenia animacji
                Log.d("SuccessActivity", "Animation repeated")
            }
        })
    }
}

