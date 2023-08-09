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

class UserProfileActivity2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var textViewWorkoutMethod2: TextView
    private var profileImageUri: Uri? = null
    private var currentPhotoPath: String = ""
    private lateinit var userId: String
    private lateinit var invitationId: String

    companion object {
        const val REQUEST_TAKE_PHOTO = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile2)
        profileImageView = findViewById(R.id.profile_image2)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get userId and invitationId from the intent
        userId = intent.getStringExtra("userId") ?: ""
        invitationId = intent.getStringExtra("invitationId") ?: ""

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }
        textViewWorkoutMethod2 = findViewById(R.id.userTextViewWorkoutMethod2)

        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)

        acceptButton.setOnClickListener {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                // Update current user's friend list
                val currentUserRef = db.collection("users").document(currentUserId)
                currentUserRef.update(
                    "friends",
                    FieldValue.arrayUnion(userId)
                )
                    .addOnSuccessListener {
                        Log.d(
                            "UserProfileActivity2",
                            "Successfully added friend"
                        )
                        // Update the other user's friend list
                        val otherUserRef = db.collection("users").document(userId)
                        otherUserRef.update("friends", FieldValue.arrayUnion(currentUserId))
                            .addOnSuccessListener {
                                Log.d(
                                    "UserProfileActivity2",
                                    "Successfully updated friend's friends list"
                                )
                                // Delete invitation
                                db.collection("invitations").document(invitationId)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this@UserProfileActivity2, "Zaproszenie przyjęte", Toast.LENGTH_SHORT).show()

                                        // Start DashboardActivity
                                        val intent = Intent(this@UserProfileActivity2, DashboardActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        startActivity(intent)
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.w("UserProfileActivity2", "Error deleting invitation", exception)
                                    }
                            }
                            .addOnFailureListener { exception ->
                                Log.e(
                                    "UserProfileActivity2",
                                    "Error updating friend's friends list",
                                    exception
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            "UserProfileActivity2",
                            "Error updating current user's friends list",
                            exception
                        )
                    }
            }
        }

        rejectButton.setOnClickListener {
            // Delete invitation
            db.collection("invitations").document(invitationId)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this@UserProfileActivity2, "Zaproszenie odrzucone", Toast.LENGTH_SHORT).show()

                    // Start DashboardActivity
                    val intent = Intent(this@UserProfileActivity2, DashboardActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
                .addOnFailureListener { exception ->
                    Log.w("UserProfileActivity2", "Error rejecting invitation", exception)
                }
        }

        checkPrivacySettings()
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
                Log.d("UserProfileActivity2", "textViewWorkoutMethod: $textViewWorkoutMethod2")
                textViewWorkoutMethod2.text = workoutMethod
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

    private fun updateInvitationStatus(newStatus: String) {
        db.collection("invitations").document(invitationId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Invitation $newStatus", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                Log.w("UserProfileActivity2", "Error updating invitation", exception)
            }
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

                    Log.d("UserProfileActivity2", "isPrivacyEnabled: $isPrivacyEnabled")

                    if (isPrivacyEnabled) {
                        // Wyświetlamy dialog, że nie możemy zobaczyć profilu
                        Log.d("UserProfileActivity2", "Showing dialog because profile is private")
                        showDialog()
                    } else {
                        // Jeśli prywatność nie jest ustawiona, ładujemy profil
                        Log.d("UserProfileActivity2", "Loading user profile because profile is not private")
                        loadUserProfile()
                    }
                } else {
                    Log.d("UserProfileActivity2", "No user settings found, loading user profile")
                    loadUserProfile()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserProfileActivity2", "Failed to load user settings", e)
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
