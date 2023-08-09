package com.example.gympassion

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.gympassion.databinding.ActivityRegistrationBinding
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var viewModel: RegistrationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "Rejestracja"

        val editTextName = findViewById<EditText>(R.id.editTextName)
        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextNickname = findViewById<EditText>(R.id.editTextNickname)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val editTextConfirmPassword = findViewById<EditText>(R.id.editTextConfirmPassword)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)
        val buttonGoToLogin = findViewById<Button>(R.id.buttonGoToLogin)



        viewModel = ViewModelProvider(this).get(RegistrationViewModel::class.java)

        // Powiązanie ViewModelu z widokiem.
        binding.viewModel = viewModel

        // Ustawienie właściciela cyklu życia dla LiveData.
        binding.lifecycleOwner = this

        buttonGoToLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val auth = FirebaseAuth.getInstance()
        Log.d("RegistrationActivity", "ViewModel created: $viewModel")

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

        val passwordTextInputLayout = findViewById<TextInputLayout>(R.id.passwordTextInputLayout)
        passwordTextInputLayout.setEndIconOnClickListener {
            val message =
                "Hasło musi zawierać co najmniej 8 znaków, w tym przynajmniej jedną dużą literę, jedną małą literę, jedną cyfrę oraz jeden znak specjalny."

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Informacje o haśle")

            // Ustawienie niestandardowego wyglądu
            val textView = TextView(this)
            textView.text = message
            textView.textSize = 16f

            // Sprawdź tryb aktualnego motywu i ustaw odpowiedni kolor tekstu
            val currentNightMode =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                // Tryb ciemny - ustaw kolor tekstu na kolor zasobu "white" dla trybu ciemnego
                textView.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                // Tryb jasny - ustaw kolor tekstu na czarny
                textView.setTextColor(Color.BLACK)
            }


            // Dodanie paddingu
            val paddingInPixels = 50 // Ustaw wartość paddingu
            textView.setPadding(paddingInPixels, paddingInPixels, paddingInPixels, paddingInPixels)

            // Ustawienie TextView jako treści AlertDialog
            builder.setView(textView)

            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()

            // Zmiana pozycji AlertDialog
            dialog.window?.attributes?.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            dialog.window?.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            dialog.show()
        }


        buttonRegister.setOnClickListener {
            val name = editTextName.text.toString()
            val nickname = editTextNickname.text.toString()
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()
            val confirmPassword = editTextConfirmPassword.text.toString()

            Log.d(
                "RegistrationActivity",
                "User input: email: $email, password: $password, confirm password: $confirmPassword"
            )

            if (nickname.isEmpty()) {
                shakeEditText(editTextNickname)
                Toast.makeText(this, "Wpisz nazwę użytkownika", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                shakeEditText(editTextName)
                Toast.makeText(this, "Wpisz imię", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.length < 2 || name.length > 50) {
                shakeEditText(editTextName)
                Toast.makeText(this, "Imię powinno mieć od 2 do 50 znaków", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (!name.all { it.isLetter() || it == ' ' || it == '-' }) {
                shakeEditText(editTextName)
                Toast.makeText(
                    this,
                    "Imię powinno zawierać tylko litery, spacje lub myślniki",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                shakeEditText(editTextEmail)
                Toast.makeText(this, "Wpisz e-mail", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                shakeEditText(editTextPassword)
                Toast.makeText(this, "Wpisz hasło", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                shakeEditText(editTextConfirmPassword)
                Toast.makeText(this, "Potwierdź hasło", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                shakeEditText(editTextPassword)
                shakeEditText(editTextConfirmPassword)
                Log.w("RegistrationActivity", "Passwords are not matching")
                Toast.makeText(this, "Hasła nie są zgodne.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val animationView = findViewById<LottieAnimationView>(R.id.animationView)

            val db = FirebaseFirestore.getInstance()
            val nicknameQuery = db.collection("users").whereEqualTo("nickname", nickname)
            nicknameQuery.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (!document.isEmpty) {
                        // Pseudonim użytkownika już istnieje, poinformuj użytkownika
                        Toast.makeText(
                            this,
                            "Nazwa użytkownika już istnieje.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val db = FirebaseFirestore.getInstance()
                                    val user = hashMapOf(
                                        "name" to name.capitalize(),
                                        "nickname" to nickname,
                                        "email" to email
                                    )

                                    val settings = hashMapOf(
                                        "notifications" to false,
                                        "metric" to false,
                                        "darkMode" to false,
                                        "firstSettingsChange" to true
                                    )

                                    auth.currentUser?.sendEmailVerification()
                                        ?.addOnCompleteListener {
                                            if (it.isSuccessful) {
                                                // Wiadomość e-mail została wysłana
                                                Toast.makeText(
                                                    this,
                                                    "Wysłano wiadomość e-mail weryfikacyjną.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                // Wysłanie wiadomości e-mail nie powiodło się
                                                Toast.makeText(
                                                    this,
                                                    "Nie udało się wysłać wiadomości e-mail weryfikacyjnej.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }



                                    db.collection("users")
                                        .document(auth.currentUser!!.uid)
                                        .set(user)
                                        .addOnSuccessListener {
                                            Log.d(
                                                TAG,
                                                "User info successfully written!"
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(
                                                TAG,
                                                "Error writing user info document",
                                                e
                                            )
                                        }

                                    db.collection("users")
                                        .document(auth.currentUser!!.uid)
                                        .collection("settings")
                                        .document("userSettings")
                                        .set(settings)
                                        .addOnSuccessListener {
                                            Log.d(
                                                TAG,
                                                "User settings successfully written!"
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(
                                                TAG,
                                                "Error writing user settings document",
                                                e
                                            )
                                        }
                                    animationView.speed = 2.0f
                                    Log.d("RegistrationActivity", "Registration successful")
                                    Toast.makeText(
                                        this,
                                        "Rejestracja przebiegła pomyślnie",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    animationView.visibility = View.VISIBLE
                                    animationView.playAnimation()
                                    animationView.addAnimatorListener(object :
                                        AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                            val intent =
                                                Intent(
                                                    this@RegistrationActivity,
                                                    MainActivity::class.java
                                                )
                                            startActivity(intent)
                                        }
                                    })

                                } else {
                                    if (task.exception is FirebaseAuthUserCollisionException) {
                                        Toast.makeText(
                                            this,
                                            "Użytkownik z tym adresem e-mail już istnieje.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Log.e(
                                            "RegistrationActivity",
                                            "Registration failed.",
                                            task.exception
                                        )
                                        Toast.makeText(
                                            this,
                                            "Rejestracja nie powiodła się.",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                }
                            }

                    }
                } else {
                    Log.d(
                        "RegistrationActivity",
                        "Failed to check if nickname exists.",
                        task.exception
                    )

                }
            }
        }
    }}