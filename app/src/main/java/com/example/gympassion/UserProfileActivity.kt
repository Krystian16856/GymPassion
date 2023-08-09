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

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var inviteButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var textViewWorkoutMethod: TextView
    private var workoutMethod: String? = null
    private var profileImageUri: Uri? = null
    private var currentPhotoPath: String = ""
    private lateinit var userId: String

    companion object {
        const val REQUEST_TAKE_PHOTO = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
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
        textViewWorkoutMethod = findViewById(R.id.userTextViewWorkoutMethod)


        inviteButton = findViewById(R.id.buttonInvite)

        setInviteButtonOnClickListener()

        checkPrivacySettings()
    }

    private fun setInviteButtonOnClickListener() {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid
        val receiverId = userId // userId użytkownika, którego profil jest przeglądany

        if (senderId != null) {
            // Sprawdź, czy istnieje już zaproszenie wysłane przez zalogowanego użytkownika
            FirebaseFirestore.getInstance().collection("invitations")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Nie znaleziono zaproszenia, ustaw tekst na "Zaproś do znajomych"
                        inviteButton.text = "Zaproś do znajomych"

                        // Ustaw OnClickListener, który pozwala wysłać zaproszenie
                        inviteButton.setOnClickListener {
                            val invitationId = if (senderId < receiverId) {
                                "${senderId}_${receiverId}"
                            } else {
                                "${receiverId}_${senderId}"
                            }

                            val invitation = hashMapOf(
                                "senderId" to senderId,
                                "receiverId" to receiverId,
                                "status" to "pending"
                            )

                            FirebaseFirestore.getInstance().collection("invitations")
                                .document(invitationId)
                                .set(invitation)
                                .addOnSuccessListener {
                                    Log.d(
                                        "UserProfileActivity",
                                        "Invitation sent with ID: $invitationId"
                                    )
                                    // Zmień tekst na "Anuluj zaproszenie"
                                    inviteButton.text = "Anuluj zaproszenie"
                                    // Ustaw nowy OnClickListener dla inviteButton, który pozwala anulować zaproszenie
                                    this@UserProfileActivity.setInviteButtonOnClickListener()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(
                                        "UserProfileActivity",
                                        "Error sending invitation",
                                        e
                                    )
                                }
                        }
                    } else {
                        // Zaproszenie istnieje i oczekuje na zaakceptowanie, ustaw tekst na "Anuluj zaproszenie"
                        inviteButton.text = "Anuluj zaproszenie"
                        // Ustaw OnClickListener, który pozwala anulować zaproszenie
                        inviteButton.setOnClickListener {
                            val invitationId = documents.documents[0].id

                            // Stwórz AlertDialog do potwierdzenia anulowania zaproszenia
                            AlertDialog.Builder(this)
                                .setTitle("Anuluj zaproszenie")
                                .setMessage("Czy na pewno chcesz anulować zaproszenie?")
                                .setPositiveButton("Tak") { dialog, _ ->
                                    FirebaseFirestore.getInstance().collection("invitations")
                                        .document(invitationId)
                                        .delete()
                                        .addOnSuccessListener {
                                            Log.d(
                                                "UserProfileActivity",
                                                "Invitation deleted with ID: $invitationId"
                                            )
                                            // Zmień tekst na "Zaproś do znajomych"
                                            inviteButton.text = "Zaproś do znajomych"
                                            // Ustaw OnClickListener z powrotem na oryginalny, który pozwala wysłać zaproszenie
                                            this@UserProfileActivity.setInviteButtonOnClickListener()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(
                                                "UserProfileActivity",
                                                "Error deleting invitation",
                                                e
                                            )
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
                .addOnFailureListener { e ->
                    Log.w(
                        "UserProfileActivity",
                        "Error checking for existing invitations",
                        e
                    )
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
                val workoutMethod = user?.get("workoutMethod") as? String
                Log.d("ProfileActivity", "Height from Firestore: $height")
                val birthDate = user?.get("birthDate") as? com.google.firebase.Timestamp
                val gym = user?.get("gym") as? String
                val description = user?.get("description") as? String

                // Update UI
                val textViewName = findViewById<TextView>(R.id.textViewName2)
                Log.d("UserProfileActivity", "textViewName: $textViewName")
                textViewName?.text = name

                val editTextHeight = findViewById<TextView>(R.id.editTextHeight2)
                Log.d("UserProfileActivity", "editTextHeight: $editTextHeight")
                editTextHeight?.text = height?.let { "${it} cm" }

                val editTextAge = findViewById<TextView>(R.id.editTextAge2)
                Log.d("UserProfileActivity", "editTextAge: $editTextAge")
                editTextAge?.text = birthDate?.let {
                    "${calculateAge(it.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())} lat"
                }

                val editTextDescription = findViewById<TextView>(R.id.editTextDescription2)
                Log.d("UserProfileActivity", "editTextDescription: $editTextDescription")
                editTextDescription?.text = description

                val gymTextView = findViewById<TextView>(R.id.textViewGym2)
                Log.d("UserProfileActivity", "gymTextView: $gymTextView")
                gymTextView?.text = if (gym.isNullOrBlank()) "Nie wybrano siłowni" else gym

                Log.d("UserProfileActivity", "workoutMethod value: $workoutMethod")
                Log.d("UserProfileActivity", "textViewWorkoutMethod: $textViewWorkoutMethod")
                textViewWorkoutMethod.text = workoutMethod
                Log.d("UserProfileActivity", "After setting workoutMethod text")

                // Load profile image
                val storageRef =
                    FirebaseStorage.getInstance().reference.child("profileImages/$userId")
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

    private fun checkPrivacySettings() {
        db.collection("users")
            .document(userId)
            .collection("settings")
            .document("userSettings")
            .get()
            .addOnSuccessListener { document ->
                val userSettings = document.data
                if (userSettings != null) {
                    val isPrivacyEnabled = userSettings["privacy"] as Boolean? ?: false

                    Log.d("UserProfileActivity", "isPrivacyEnabled: $isPrivacyEnabled")

                    if (isPrivacyEnabled) {
                        // Wyświetlamy dialog, że nie możemy zobaczyć profilu
                        Log.d("UserProfileActivity", "Showing dialog because profile is private")
                        showDialog()
                    } else {
                        // Jeśli prywatność nie jest ustawiona, ładujemy profil
                        Log.d("UserProfileActivity", "Loading user profile because profile is not private")
                        loadUserProfile()
                    }
                } else {
                    Log.d("UserProfileActivity", "No user settings found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserProfileActivity", "Failed to load user settings", e)
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
