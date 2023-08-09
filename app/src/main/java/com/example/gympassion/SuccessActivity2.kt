package com.example.gympassion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.airbnb.lottie.LottieAnimationView
import soup.neumorphism.NeumorphButton

class SuccessActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success2)
        title = "Success"

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        val goToDashboardButton: NeumorphButton = findViewById(R.id.go_to_dashboard_button)
        val animationView: LottieAnimationView = findViewById(R.id.animationView)

        goToDashboardButton.setOnClickListener {
            animationView.speed = 2.0f  // ustaw szybkość animacji
            animationView.visibility = View.VISIBLE
            animationView.playAnimation() // rozpocznij animację
            animationView.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val intent = Intent(this@SuccessActivity2, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            })
        }
    }
}
