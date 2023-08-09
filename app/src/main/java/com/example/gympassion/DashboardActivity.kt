package com.example.gympassion

import android.app.ActivityManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Tasks
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import soup.neumorphism.NeumorphCardView
import soup.neumorphism.NeumorphImageButton
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException

class DashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var logoutButton: NeumorphImageButton
    private val channelId = "TrainingNotificationChannel"

    companion object {
        var isInDashboardActivity = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val navigationView: NavigationView = findViewById(R.id.nav_view)



        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                R.id.nav_profile -> {
                    navigateToProfile()
                    true
                }

                R.id.action_delete_account -> {
                    Log.d("DashboardActivity", "Delete account menu item selected")
                    showDeleteAccountDialog()
                    true
                }
                R.id.nav_max_exercises -> {
                    Log.d("DashboardActivity", "View max weights menu item selected")
                    val intent = Intent(this, MaxWeightsActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_bmi_calculator -> {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val docRef = db.collection("users").document(userId)
                        docRef.get().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = task.result?.data
                                if (user?.get("weight") != null &&
                                    user?.get("sex") != null &&
                                    user?.get("activityLevel") != null &&
                                    user?.get("trainingLevel") != null &&
                                    user?.get("height") != null &&
                                    user?.get("birthDate") != null
                                ) {
                                    // wszystkie dane są dostępne, więc przenosimy do BMIResultsActivity
                                    val intent = Intent(this, BMIResultsActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    // nie wszystkie dane są dostępne, więc przenosimy do BMICalculatorActivity
                                    val intent = Intent(this, BMICalculatorActivity::class.java)
                                    startActivity(intent)
                                }
                            } else {
                                Log.e("DashboardActivity", "Failed to fetch user data", task.exception)
                                // obsługa błędu
                            }
                        }
                    }
                    true
                }

                R.id.nav_friends -> {
                    Log.d("DashboardActivity", "View friends menu item selected")
                    val intent = Intent(this, FriendsActivity::class.java)
                    startActivity(intent)
                    true
                }



                else -> super.onOptionsItemSelected(menuItem)
            }
        }

        title = "Pulpit"


        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        // Sprawdź, czy istnieją jakiekolwiek zaproszenia do przyjaciół dla obecnie zalogowanego użytkownika
        if (userId != null) {
            checkInvitations(userId)
        }

        val settingsButton = findViewById<NeumorphCardView>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            val userId = auth.currentUser?.uid
            if (userId != null) {
                intent.putExtra("userId", userId)
            }
            startActivity(intent)
        }

        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            showLogoutDialog()
        }
        if (userId != null) {
            db.collection("users")
                .document(userId)
                .collection("settings")
                .document("userSettings")
                .get()
                .addOnSuccessListener { document ->
                    val userSettings = document.data
                    val notificationsEnabled = userSettings?.get("notifications") as? Boolean
                    Log.d("DashboardActivity", "User settings loaded: $userSettings")
                }
                .addOnFailureListener { exception ->
                    Log.e("DashboardActivity", "Failed to load user settings", exception)
                    }

        } else {
            }



    }

    override fun onResume() {
        super.onResume()
        isInDashboardActivity = true

        val welcomeText: TextView = findViewById(R.id.welcome_text)
        val greetings = resources.getStringArray(R.array.greetings)

        val userIcon: ImageView = findViewById(R.id.user_icon)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)

        userIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        val userId = auth.currentUser?.uid
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId")

        Handler(Looper.getMainLooper()).postDelayed({
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(userIcon)
            }.addOnFailureListener {
                // Handle any errors
                Glide.with(this)
                    .load(R.drawable.user)
                    .circleCrop()
                    .into(userIcon)
            }
        }, 150)







    val fonts = arrayOf(
            "OpenSans-SemiboldItalic.ttf",
            "OpenSans-LightItalic.ttf",
            "OpenSans-Italic.ttf",
            "OpenSans-BoldItalic.ttf",
            "OpenSans-Bold.ttf"
        )

        val newUserButton: NeumorphImageButton = findViewById(R.id.newUserButton)
        newUserButton.setOnClickListener {
            // Użytkownik kliknął "newUserButton", otwórz InvitationsActivity
            val intent = Intent(this, InvitationsActivity::class.java)
            startActivity(intent)
        }

        val changeWorkoutPlanButton = findViewById<NeumorphCardView>(R.id.change_workout_plan_button)
        changeWorkoutPlanButton.setOnClickListener {
            val intent = Intent(this, EditPlanWorkoutActivity::class.java)
            startActivity(intent)
        }
        val startWorkoutButton: NeumorphCardView = findViewById(R.id.start_workout_button)
        startWorkoutButton.setOnClickListener {
            val intent = Intent(this, StartWorkoutActivity::class.java)
            startActivity(intent)
        }

        val workoutHistoryButton: NeumorphCardView = findViewById(R.id.workout_history_button)
        workoutHistoryButton.setOnClickListener {
            val intent = Intent(this, WorkoutHistoryActivity::class.java)
            startActivity(intent)
        }

        val colors = resources.getIntArray(R.array.colors)

        if (userId != null) {
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.data
                    val username = user?.get("name") as? String
                    if (username != null) {
                        val randomGreeting = greetings[(greetings.indices).random()]
                        val finalGreeting = String.format(randomGreeting, username)

                        val randomFont = fonts[(fonts.indices).random()]
                        val typeface = Typeface.createFromAsset(assets, "fonts/$randomFont")
                        welcomeText.typeface = typeface

                        // Check if dark mode is enabled
                        val isDarkTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                        val chosenColor = if (isDarkTheme) Color.YELLOW else Color.RED

                        val spannable = SpannableString(finalGreeting)
                        val start = finalGreeting.indexOf(username)
                        val end = start + username.length
                        spannable.setSpan(ForegroundColorSpan(chosenColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        welcomeText.text = spannable
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this@DashboardActivity, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
        }

        checkForUnreadMessages()
    }

    override fun onPause() {
        super.onPause()
        isInDashboardActivity = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_drawer, menu)
        return true
    }

    fun checkForUnreadMessages() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val newMessageButton = findViewById<NeumorphImageButton>(R.id.newMessageButton)

        newMessageButton.setOnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            startActivity(intent)
        }

        if (currentUserId != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    val friendIds = document.get("friends") as? List<String> ?: listOf()
                    for (friendId in friendIds) {
                        val chatId = getChatId(currentUserId, friendId)
                        FirebaseFirestore.getInstance().collection("chats")
                            .document(chatId)
                            .collection("messages")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .addSnapshotListener { snapshots, e ->
                                if (e != null) {
                                    Log.w("DashboardActivity", "Listen failed.", e)
                                    return@addSnapshotListener
                                }

                                if (snapshots != null && !snapshots.isEmpty) {
                                    for (document in snapshots.documents) {
                                        val readBy = document.get("readBy") as? List<String> // rzutujemy na List<String>
                                        if (readBy?.contains(currentUserId) == false) { // jeśli currentUserId nie jest na liście
                                            newMessageButton.visibility = View.VISIBLE
                                            return@addSnapshotListener
                                        }
                                    }
                                }
                                newMessageButton.visibility = View.GONE
                            }
                    }
                }
        }
    }

    private fun getChatId(userId1: String, userId2: String): String {
        val userIds = listOf(userId1, userId2).sorted()
        return userIds.joinToString("_")
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("DashboardActivity", "Menu item selected: ${item.itemId}")
        return when (item.itemId) {
            R.id.action_delete_account -> {
                Log.d("DashboardActivity", "Delete account menu item selected")
                showDeleteAccountDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun navigateToProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.data
                    val birthDate = user?.get("birthDate")
                    val height = user?.get("height")
                    if (birthDate == null || height == null) {
                        // Przejdź do AgeHeightActivity
                        val intent = Intent(this, AgeHeightActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Przejdź do ProfileActivity
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this@DashboardActivity, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkInvitations(userId: String) {
        // Nasłuchuj zmian zaproszeń do przyjaciół dla tego użytkownika
        FirebaseFirestore.getInstance().collection("invitations")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("DashboardActivity", "Listen for invitations failed.", e)
                    return@addSnapshotListener
                }

                val newUserButton: NeumorphImageButton = findViewById(R.id.newUserButton)
                if (!snapshots!!.isEmpty) {
                    // Znaleziono zaproszenie do przyjaciół, ustaw przycisk newUserButton na widoczny
                    newUserButton.visibility = View.VISIBLE
                } else {
                    // Nie znaleziono zaproszenia do przyjaciół, ustaw przycisk newUserButton na niewidoczny
                    newUserButton.visibility = View.GONE
                }
            }
    }



    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Czy na pewno chcesz się wylogować?")
            .setPositiveButton("Tak") { _, _ ->
                logout()
            }
            .setNegativeButton("Nie", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Usuń konto")
            .setMessage("Czy na pewno chcesz usunąć swoje konto? Wszystkie dane zostaną utracone.")
            .setPositiveButton("Tak") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Nie", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        user?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Konto zostało usunięte.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Nie udało się usunąć konta.", Toast.LENGTH_SHORT).show()
                }
            }
    }



    private fun logout() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .update("isOnline", false)
                .addOnSuccessListener {
                    Log.d("DashboardActivity", "User online status updated to 'false'")

                    auth.signOut()
                    // Przejdź z powrotem do strony logowania
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.w("DashboardActivity", "Error updating user online status", e)
                }
        } else {
            Log.d("DashboardActivity", "No user is currently signed in")
        }

        // Resetuj flagę stayLoggedIn w preferencjach współdzielonych
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("stayLoggedIn", false)
            apply()
        }
        sharedPreferences.edit().apply {
            putBoolean("hasAsked", false)
            apply()
        }

    }

}
