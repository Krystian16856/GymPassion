package com.example.gympassion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoadingActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        Log.d("LoadingActivity", "onCreate called")
        loadUserDarkModePreference()

        setContentView(R.layout.activity_loading)

        Glide.with(this)
            .load(R.mipmap.ic_launcher)
            .circleCrop()
            .into(findViewById(R.id.logoImageView))

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        // Pobierz ImageView do animacji
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)

        // Wczytaj animację
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)

        // Zastosuj animację do ImageView
        logoImageView.startAnimation(pulseAnimation)



        checkStayLoggedInPreference()
    }

    private fun checkStayLoggedInPreference() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val hasAsked = sharedPreferences.getBoolean("hasAsked", false)

        if (!hasAsked) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_stay_logged_in, null)
            val checkBox = dialogView.findViewById<CheckBox>(R.id.dontAskAgainCheckBox)

            AlertDialog.Builder(this)
                .setTitle("Zostań zalogowany")
                .setMessage("Czy chcesz pozostać zalogowany?")
                .setView(dialogView)
                .setPositiveButton("Tak") { _, _ ->
                    sharedPreferences.edit().apply {
                        putBoolean("stayLoggedIn", true)
                        if (checkBox.isChecked) putBoolean("hasAsked", true)
                        apply()
                    }
                    loadDataFromIntent()
                }
                .setNegativeButton("Nie") { _, _ ->
                    sharedPreferences.edit().apply {
                        putBoolean("stayLoggedIn", false)
                        if (checkBox.isChecked) putBoolean("hasAsked", true)
                        apply()
                    }
                    loadDataFromIntent()
                }
                .show()
        } else {
            loadDataFromIntent()
        }
    }

    private fun loadDataFromIntent() {
        val usernameOrEmail = intent.getStringExtra("usernameOrEmail")
        val password = intent.getStringExtra("password")
        if (usernameOrEmail != null && password != null) {
            loadData(usernameOrEmail, password)
        } else {
            // Jeśli nie ma danych logowania w intencji, załaduj dane użytkownika bezpośrednio
            loadUserData()
        }
    }




    private fun loadData(usernameOrEmail: String, password: String) {
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(usernameOrEmail).matches()) {
            // The input is email, proceed with normal email and password login
            auth.signInWithEmailAndPassword(usernameOrEmail, password)
                .addOnSuccessListener {
                    // Logowanie powiodło się, kontynuuj ładowanie danych
                    loadUserData()
                }
                .addOnFailureListener {
                    // Logowanie nie powiodło się, pokaż błąd
                    Toast.makeText(this, "Nie udało się zalogować.", Toast.LENGTH_SHORT).show()
                    Log.d("LoadingActivity", "Failed to login with email and password.")
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
                                        // Logowanie powiodło się, kontynuuj ładowanie danych
                                        loadUserData()
                                    } else {
                                        // Logowanie nie powiodło się, pokaż błąd
                                        Toast.makeText(
                                            this,
                                            "Nie udało się zalogować.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            // Something went wrong, the email field is missing
                            Toast.makeText(
                                this,
                                "Nie udało się uzyskać adresu e-mail.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors here
                    Log.d("LoadingActivity", "Error getting documents: ", exception)
                }
        }
    }

    private fun loadUserDarkModePreference() {
        Log.d("LoadingActivity", "loadUserDarkModePreference started")
        val user = auth.currentUser
        if (user != null) {
            val userSettingsDocRef = db.collection("users").document(user.uid).collection("settings").document("userSettings")
            userSettingsDocRef.get().addOnSuccessListener { document ->
                val userSettings = document.data
                if (userSettings != null) {
                    Log.d("LoadingActivity", "User is logged in: ${user.email}")
                    val darkModeOn = userSettings["darkMode"] as Boolean? ?: false
                    Log.d("LoadingActivity", "DarkMode from Firebase: $darkModeOn")
                    if (darkModeOn) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("LoadingActivity", "Error fetching dark mode setting", e)
            }
        } else {
            Log.d("LoadingActivity", "User not logged in")
        }
    }




    private fun loadUserData() {
        // Pobierz dane użytkownika z Firestore
        Log.d("LoadingActivity", "loadUserData started") // <-- Dodaj ten log
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    Log.d("LoadingActivity", "UserData fetched successfully") // <-- Dodaj ten log
                    // Dane użytkownika zostały pobrane, przejdź do DashboardActivity
                    onDataLoaded()
                }
                .addOnFailureListener { e ->
                    // Nie udało się pobrać danych użytkownika, pokaż błąd
                    Toast.makeText(
                        this,
                        "Nie udało się pobrać danych użytkownika.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("LoadingActivity", "Failed to fetch user data: ", e)
                }
        } else {
            // Brak zalogowanego użytkownika, pokaż błąd
            Toast.makeText(this, "Nie jesteś zalogowany.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onDataLoaded() {
        Log.d("LoadingActivity", "onDataLoaded called") // <-- Dodaj ten log
        // Wszystkie dane zostały załadowane, przejdź do DashboardActivity
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}