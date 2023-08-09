package com.example.gympassion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import soup.neumorphism.NeumorphCardView
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.Period
import java.util.*

class UserProfileActivity3 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var inviteButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var textViewWorkoutMethod3: TextView
    private var profileImageUri: Uri? = null
    private var currentPhotoPath: String = ""
    private lateinit var userId: String

    companion object {
        const val REQUEST_TAKE_PHOTO = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile3)
        profileImageView = findViewById(R.id.profile_image2)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get userId from the intent
        userId = intent.getStringExtra("userId") ?: ""

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }
        textViewWorkoutMethod3 = findViewById(R.id.userTextViewWorkoutMethod3)

        inviteButton = findViewById(R.id.buttonInvite)

        setInviteButtonOnClickListener()

        checkPrivacyFriendsSettings()
    }

    private fun setInviteButtonOnClickListener() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val friendId = userId

        if (currentUserId != null) {
            inviteButton.text = "Usuń ze znajomych"
            inviteButton.setOnClickListener {
                // Stwórz AlertDialog do potwierdzenia usunięcia znajomego
                AlertDialog.Builder(this)
                    .setTitle("Usuń znajomego")
                    .setMessage("Czy na pewno chcesz usunąć tego użytkownika ze swojej listy znajomych?")
                    .setPositiveButton("Tak") { dialog, _ ->
                        // Usuń friendId z listy znajomych currentUser
                        FirebaseFirestore.getInstance().collection("users").document(currentUserId).update("friends", FieldValue.arrayRemove(friendId))
                        // Usuń currentUserId z listy znajomych friendId
                        FirebaseFirestore.getInstance().collection("users").document(friendId).update("friends", FieldValue.arrayRemove(currentUserId))
                            .addOnSuccessListener {
                                Log.d("UserProfileActivity3", "Successfully removed friend")
                                // Zaktualizuj tekst przycisku lub przejdź do innego ekranu
                                inviteButton.text = "Zaproś do znajomych"
                                // Ustaw OnClickListener z powrotem na oryginalny, który pozwala wysłać zaproszenie
                                this@UserProfileActivity3.setInviteButtonOnClickListener()
                                // Przejdź do DashboardActivity
                                startActivity(Intent(this, DashboardActivity::class.java))
                            }
                            .addOnFailureListener { e ->
                                Log.w("UserProfileActivity3", "Error removing friend", e)
                            }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Nie") { dialog, _ ->
                        // Zamknij dialog i nie rób nic
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }




    private fun loadUserProfile() {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.data
                val name = user?.get("name") as? String
                val height = user?.get("height") as? Long
                val birthDate = user?.get("birthDate") as? com.google.firebase.Timestamp
                val description = user?.get("description") as? String
                val gym = user?.get("gym") as? String
                val workoutMethod = user?.get("workoutMethod") as? String

                // Update UI
                val textViewName = findViewById<TextView>(R.id.textViewName2)
                Log.d("UserProfileActivity2", "textViewName: $textViewName")
                textViewName?.text = name

                val editTextHeight = findViewById<TextView>(R.id.editTextHeight2)
                Log.d("UserProfileActivity2", "editTextHeight: $editTextHeight")
                editTextHeight?.text = height?.let { "${it} cm" }

                val editTextAge = findViewById<TextView>(R.id.editTextAge2)
                Log.d("UserProfileActivity2", "editTextAge: $editTextAge")
                editTextAge?.text = birthDate?.let {
                    "${calculateAge(it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())} lat"
                }

                val editTextDescription = findViewById<TextView>(R.id.editTextDescription2)
                Log.d("UserProfileActivity2", "editTextDescription: $editTextDescription")
                editTextDescription?.text = description

                val gymTextView = findViewById<TextView>(R.id.textViewGym2)
                Log.d("UserProfileActivity2", "gymTextView: $gymTextView")
                gymTextView?.text = if (gym.isNullOrBlank()) "Nie wybrano siłowni" else gym

                Log.d("UserProfileActivity2", "workoutMethod value: $workoutMethod")
                Log.d("UserProfileActivity2", "textViewWorkoutMethod: $textViewWorkoutMethod3")
                textViewWorkoutMethod3.text = workoutMethod
                Log.d("UserProfileActivity2", "After setting workoutMethod text")

                // Load profile image
                val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId")
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .into(profileImageView)
                }.addOnFailureListener {
                    // On failure, load default image
                    Glide.with(this)
                        .load(R.drawable.user) // Assuming 'user' is the default image in your app's resources
                        .into(profileImageView)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to load user data: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun calculateAge(birthDate: LocalDate): String {
        val now = LocalDate.now()
        return "${Period.between(birthDate, now).years}"
    }

    private fun checkPrivacyFriendsSettings() {
        db.collection("users")
            .document(userId)
            .collection("settings")
            .document("userSettings")
            .get()
            .addOnSuccessListener { document ->
                val userSettings = document.data
                if (userSettings != null) {
                    val isPrivacyEnabled = userSettings["privacy"] as Boolean? ?: false
                    val isPrivacyFriendsEnabled = userSettings["privacyFriends"] as Boolean? ?: false

                    Log.d("UserProfileActivity3", "isPrivacyEnabled: $isPrivacyEnabled")
                    Log.d("UserProfileActivity3", "isPrivacyFriendsEnabled: $isPrivacyFriendsEnabled")

                    if (isPrivacyEnabled && !isPrivacyFriendsEnabled) {
                        // Wyświetlamy dialog, że nie możemy zobaczyć profilu
                        Log.d("UserProfileActivity3", "Showing dialog because profile is private and not available for friends")
                        showDialog()
                    } else {
                        // Jeśli prywatność nie jest ustawiona lub prywatność dla znajomych jest włączona, ładujemy profil
                        Log.d("UserProfileActivity3", "Loading user profile because profile is not private or it's available for friends")
                        loadUserProfile()
                    }
                } else {
                    Log.d("UserProfileActivity3", "No user settings found, loading user profile")
                    loadUserProfile()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserProfileActivity3", "Failed to load user settings", e)
            }
    }


    private fun showDialog() {
        AlertDialog.Builder(this)
            .setTitle("Profil jest prywatny")
            .setMessage("Nie możesz wyświetlić tego profilu, ponieważ jest on ustawiony jako prywatny.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }
}
