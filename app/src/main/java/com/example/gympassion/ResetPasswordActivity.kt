package com.example.gympassion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.gympassion.MainActivity
import com.example.gympassion.databinding.ActivityResetPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetPasswordBinding
    private val viewModel: ResetPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "Resetowanie hasła"

        val auth = FirebaseAuth.getInstance()
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val buttonResetPassword = findViewById<Button>(R.id.buttonResetPassword)

        fun shakeEditText(editText: EditText) {
            val rotate = RotateAnimation(
                -5f,
                5f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate.duration = 100
            rotate.repeatCount = 5
            rotate.repeatMode = Animation.REVERSE

            val scale = ScaleAnimation(
                1f,
                1.1f,
                1f,
                1.1f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            scale.duration = 100
            scale.repeatCount = 5
            scale.repeatMode = Animation.REVERSE

            val shake = AnimationSet(true)
            shake.addAnimation(rotate)
            shake.addAnimation(scale)
            editText.startAnimation(shake)
        }


        buttonResetPassword.setOnClickListener {
            val email = editTextEmail.text.toString()

            val animationView = findViewById<LottieAnimationView>(R.id.animationView)

            if (email.isEmpty()) {
                shakeEditText(editTextEmail)
                Toast.makeText(this, "Wpisz e-mail", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            animationView.speed = 2.0f
                            Toast.makeText(
                                this,
                                "Wysłano link na adres mailowy!",
                                Toast.LENGTH_SHORT
                            ).show()
                            animationView.visibility = View.VISIBLE
                            animationView.playAnimation()
                            animationView.addAnimatorListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    Log.d("ResetPasswordActivity", "Email sent.")
                                    val intent =
                                        Intent(this@ResetPasswordActivity, MainActivity::class.java)
                                    startActivity(intent)
                                }
                            })
                        } else {
                            Log.e(
                                "ResetPasswordActivity",
                                "Failed to send email.",
                                task.exception
                            )
                            Toast.makeText(this, "Nie ma takiego Email..", Toast.LENGTH_SHORT)
                                .show()

                        }
                    }
            } else {
                Toast.makeText(this, "Wprowadź swój adres e-mail", Toast.LENGTH_LONG).show()
            }
        }

        binding.buttonGoBack.setOnClickListener {
            // Navigate back to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
