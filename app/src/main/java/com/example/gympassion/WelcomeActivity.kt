package com.example.gympassion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.airbnb.lottie.LottieAnimationView
import soup.neumorphism.NeumorphButton

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        title = "Success"

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        val welcomeMessage = findViewById<TextView>(R.id.welcome_message)
        val createPlanButton = findViewById<NeumorphButton>(R.id.create_plan_button)
        val animationView = findViewById<LottieAnimationView>(R.id.animationView)

        val animator = ValueAnimator.ofFloat(1.0f, 1.3f, 1.0f)
        animator.duration = 2000  // duration of the animation in milliseconds
        animator.repeatCount = ValueAnimator.INFINITE  // repeat indefinitely
        animator.repeatMode = ValueAnimator.REVERSE  // reverse the animation at the end
        animator.interpolator = AccelerateDecelerateInterpolator()  // speed up in the middle, slow down at the ends
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            welcomeMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue * resources.getDimension(R.dimen.text_size))  // set the text size
        }
        animator.start()

        createPlanButton.setOnClickListener {
            animationView.speed = 2.5f
            animationView.visibility = View.VISIBLE
            animationView.playAnimation()
            animationView.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Here replace NewActivity::class.java with the activity you want to start
                    val intent = Intent(this@WelcomeActivity, CreateWorkoutPlanActivity::class.java)
                    startActivity(intent)
                }
            })
        }
    }
}
