package com.example.gympassion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    companion object {
        var isInMainActivity = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "Gym Passion"

        Glide.with(this)
            .load(R.mipmap.ic_launcher)
            .circleCrop()
            .into(findViewById(R.id.imageViewLogo))


        // W onCreate Twojej głównej aktywności
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isDarkModeSwitchLocked", false).apply()


        val editTextUsernameOrEmail = findViewById<EditText>(R.id.editTextUsernameOrEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val buttonGoToRegister = findViewById<Button>(R.id.buttonGoToRegister)
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            saveTokenToFirestore(token)
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("chat_messages_channel", "Wiadomości chatu", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val stayLoggedIn = sharedPreferences.getBoolean("stayLoggedIn", false)
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null && stayLoggedIn) {
            // Użytkownik jest zalogowany i chce zostać zalogowany, przekieruj go bezpośrednio do DashboardActivity
            val intent = Intent(this, LoadingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }


        fun shakeEditText(editText: EditText) {
            val rotate = RotateAnimation(
                -5f,
                5f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            rotate.duration = 100  // duration in milliseconds
            rotate.repeatCount = 5  // repeat the animation 5 times
            rotate.repeatMode = Animation.REVERSE  // reverse the animation at the end

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
            scale.duration = 100  // duration in milliseconds
            scale.repeatCount = 5  // repeat the animation 5 times
            scale.repeatMode = Animation.REVERSE  // reverse the animation at the end

            val shake = AnimationSet(true)
            shake.addAnimation(rotate)
            shake.addAnimation(scale)
            editText.startAnimation(shake)
        }

        buttonGoToRegister.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        buttonLogin.setOnClickListener {
            val usernameOrEmail = editTextUsernameOrEmail.text.toString()
            val password = editTextPassword.text.toString()

            if (usernameOrEmail.isEmpty()) {
                shakeEditText(editTextUsernameOrEmail)
                Toast.makeText(this, "Wpisz nazwę użytkownika lub e-mail", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }


            if (password.isEmpty()) {
                shakeEditText(editTextPassword)
                Toast.makeText(this, "Wpisz hasło", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Implement the logic to handle username login here

            if (android.util.Patterns.EMAIL_ADDRESS.matcher(usernameOrEmail).matches()) {
                // The input is email, proceed with normal email and password login
                auth.signInWithEmailAndPassword(usernameOrEmail, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user?.isEmailVerified == true) {
                                db.collection("users").document(user.uid)
                                    .update("isOnline", true)
                                    .addOnSuccessListener {
                                        Log.d("MainActivity", "User online status updated to 'true'")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("MainActivity", "Error updating user online status", e)
                                    }
                                Toast.makeText(this, "Zalogowano!", Toast.LENGTH_SHORT).show()


                                val userDocRef = db.collection("users").document(user.uid)
                                val userSettingsDocRef =
                                    db.collection("users").document(user.uid).collection("settings")
                                        .document("userSettings")

                                userSettingsDocRef.get().addOnSuccessListener { document ->
                                    val userSettings = document.data
                                    if (userSettings != null) {
                                        val darkModeOn =
                                            userSettings["darkMode"] as Boolean? ?: false
                                        if (darkModeOn) {
                                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                        } else {
                                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                        }

                                        // Check if user has a workout plan
                                        val docRef = userDocRef.collection("workoutPlans")
                                        docRef.get().addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val document = task.result
                                                if (document != null) {
                                                    if (!task.result.isEmpty) {
                                                        // Zamyka MainActivity
                                                        finish()
                                                        // User has a workout plan already, navigate to LoadingActivity
                                                        val intent = Intent(this, LoadingActivity::class.java)
                                                        intent.putExtra("usernameOrEmail", usernameOrEmail)
                                                        intent.putExtra("password", password)
                                                        startActivity(intent)
                                                    } else {
                                                        // User doesn't have a workout plan yet, navigate to CreateWorkoutPlanActivity
                                                        val intent = Intent(
                                                            this,
                                                            WelcomeActivity::class.java
                                                        )
                                                        startActivity(intent)
                                                    }
                                                }
                                            } else {
                                                Log.d(
                                                    "MainActivity",
                                                    "get failed with ",
                                                    task.exception
                                                )
                                            }
                                        }
                                    }
                                }.addOnFailureListener { exception ->
                                    Toast.makeText(
                                        this,
                                        "Failed to load settings: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                // User's email is not verified, remind them to verify
                                Toast.makeText(
                                    this,
                                    "Potwierdź swój adres e-mail przed zalogowaniem.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } else {
                            val exception = task.exception
                            if (exception is FirebaseAuthInvalidUserException) {
                                Toast.makeText(
                                    this,
                                    "Taki użytkownik nie istnieje.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (exception is FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(this, "Nieprawidłowe hasło.", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(this, "Nie udało się zalogować.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
            } else {
                // The input is a username, need to get the email associated with this username from Firestore first
                db.collection("users").whereEqualTo("nickname", usernameOrEmail).get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            // No user found with this username
                            Toast.makeText(
                                this,
                                "Nie znaleziono użytkownika o tej nazwie.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Found the user with this username, get their email
                            val email = documents.documents[0].getString("email")
                            if (email != null) {
                                // Proceed with email and password login
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(this) { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user?.isEmailVerified == true) {
                                                db.collection("users").document(user.uid)
                                                    .update("isOnline", true)
                                                    .addOnSuccessListener {
                                                        Log.d(
                                                            "MainActivity",
                                                            "User online status updated to 'true'"
                                                        )
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.w(
                                                            "MainActivity",
                                                            "Error updating user online status",
                                                            e
                                                        )
                                                    }
                                                Toast.makeText(
                                                    this,
                                                    "Zalogowano!",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                val userDocRef =
                                                    db.collection("users").document(user.uid)
                                                val userSettingsDocRef =
                                                    db.collection("users").document(user.uid)
                                                        .collection("settings")
                                                        .document("userSettings")

                                                userSettingsDocRef.get()
                                                    .addOnSuccessListener { document ->
                                                        val userSettings = document.data
                                                        if (userSettings != null) {
                                                            val darkModeOn =
                                                                userSettings["darkMode"] as Boolean?
                                                                    ?: false
                                                            if (darkModeOn) {
                                                                AppCompatDelegate.setDefaultNightMode(
                                                                    AppCompatDelegate.MODE_NIGHT_YES
                                                                )
                                                            } else {
                                                                AppCompatDelegate.setDefaultNightMode(
                                                                    AppCompatDelegate.MODE_NIGHT_NO
                                                                )
                                                            }

                                                            // Check if user has a workout plan
                                                            val docRef =
                                                                userDocRef.collection("workoutPlans")
                                                            docRef.get()
                                                                .addOnCompleteListener { task ->
                                                                    if (task.isSuccessful) {
                                                                        val document =
                                                                            task.result
                                                                        if (document != null) {
                                                                            if (!task.result.isEmpty) {
                                                                                // Zamyka MainActivity
                                                                                finish()
                                                                                // User has a workout plan already, navigate to LoadingActivity
                                                                                val intent = Intent(this, LoadingActivity::class.java)
                                                                                intent.putExtra("usernameOrEmail", usernameOrEmail)
                                                                                intent.putExtra("password", password)
                                                                                startActivity(intent)
                                                                            } else {
                                                                                // User doesn't have a workout plan yet, navigate to CreateWorkoutPlanActivity
                                                                                val intent =
                                                                                    Intent(
                                                                                        this,
                                                                                        WelcomeActivity::class.java
                                                                                    )
                                                                                startActivity(
                                                                                    intent
                                                                                )
                                                                            }
                                                                        }
                                                                    } else {
                                                                        Log.d(
                                                                            "MainActivity",
                                                                            "get failed with ",
                                                                            task.exception
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                    }.addOnFailureListener { exception ->
                                                        Toast.makeText(
                                                            this,
                                                            "Failed to load settings: ${exception.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            } else {
                                                // User's email is not verified, remind them to verify
                                                Toast.makeText(
                                                    this,
                                                    "Potwierdź swój adres e-mail przed zalogowaniem.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                        } else {
                                            val exception = task.exception
                                            if (exception is FirebaseAuthInvalidUserException) {
                                                Toast.makeText(
                                                    this,
                                                    "Taki użytkownik nie istnieje.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else if (exception is FirebaseAuthInvalidCredentialsException) {
                                                Toast.makeText(
                                                    this,
                                                    "Nieprawidłowe hasło.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    this,
                                                    "Nie udało się zalogować.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                            } else {
                                // Something went wrong, the email field is missing
                                Toast.makeText(this, "Failed to get email.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Handle any errors here
                        Log.d("MainActivity", "Error getting documents: ", exception)
                    }

            }
        }

            val buttonResetPassword = findViewById<Button>(R.id.buttonResetPassword)
            buttonResetPassword.setOnClickListener {
                Log.d("MainActivity", "Reset Password button clicked")
                val intent = Intent(this, ResetPasswordActivity::class.java)
                startActivity(intent)
            }


    }

    override fun onResume() {
        super.onResume()
        isInMainActivity = true
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val isLoggedIn = document.getBoolean("isLoggedIn") ?: false
                        if (isLoggedIn) {
                            db.collection("users").document(user.uid)
                                .update("isOnline", true)
                                .addOnSuccessListener {
                                    Log.d("MainActivity", "User online status updated to 'true'")
                                }
                                .addOnFailureListener { e ->
                                    Log.w("MainActivity", "Error updating user online status", e)
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("MainActivity", "Error getting document", e)
                }
        }


    }

  override fun onPause() {
        super.onPause()
        isInMainActivity = false
    }

    fun saveTokenToFirestore(token: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && token != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "Token successfully written to Firestore!")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing token to Firestore", e)
                }
        }
    }
    override fun onStart() {
        super.onStart()
        val auth = FirebaseAuth.getInstance()

        // Sprawdź, czy użytkownik jest zalogowany
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val stayLoggedIn = sharedPreferences.getBoolean("stayLoggedIn", false)
            if (stayLoggedIn) {
                // Jeśli użytkownik chce pozostać zalogowany, przekieruj do DashboardActivity
                val intent = Intent(this, LoadingActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }


}
