package com.example.gympassion

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.AlarmManagerCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import soup.neumorphism.NeumorphCardView


class SettingsActivity : AppCompatActivity() {

    private lateinit var notificationRow: RelativeLayout
    private lateinit var metricRow: RelativeLayout
    private lateinit var darkModeRow: RelativeLayout

    private lateinit var notificationSwitch: Switch
    private lateinit var metricSwitch: Switch
    private lateinit var darkModeSwitch: Switch

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var notificationDescription: TextView
    private lateinit var metricDescription: TextView
    private lateinit var darkModeDescription: TextView

    private lateinit var scheduledWorkoutsRow: RelativeLayout
    private lateinit var scheduledWorkoutsTitle: TextView
    private var firstSettingsChange: Boolean = false

    private lateinit var privacyRow: RelativeLayout
    private lateinit var privacySwitch: Switch
    private lateinit var privacyDescription: TextView
    private lateinit var privacyAdditionalSettings: LinearLayout
    private lateinit var privacyFriendsSwitch: Switch
    private lateinit var privacyFriendsDescription: TextView


    private lateinit var nextButton: Button

    private var isDarkModeEnabled = false
    private var darkModeSettingChanged = false
    private var isRecreating = false
    var isUserInteraction = true
    private var isRelaunchedDueToDarkModeChange = false
    var userTriedToToggle = false
    private var isSwitchLocked = false



    private val settings = mutableMapOf<String, Boolean>()
    private val EXACT_ALARM_REQUEST_CODE = 1
    private val FOREGROUND_SERVICE_REQUEST_CODE = 2

    companion object {
        var isInSettingsActivity = false


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Ustawienia"

        Log.d("SettingsActivity", "onCreate")
        isRecreating = false

        isRelaunchedDueToDarkModeChange = false

        isRecreating = savedInstanceState?.getBoolean("isRecreating", false) ?: false



        setContentView(R.layout.activity_settings)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        nextButton = findViewById(R.id.next_button)

        notificationRow = findViewById(R.id.notification_row)
        metricRow = findViewById(R.id.metric_row)
        darkModeRow = findViewById(R.id.dark_mode_row)

        notificationSwitch = findViewById(R.id.notification_switch)
        metricSwitch = findViewById(R.id.metric_switch)
        darkModeSwitch = findViewById(R.id.dark_mode_switch)

        notificationDescription = findViewById(R.id.notification_description)
        metricDescription = findViewById(R.id.metric_description)
        darkModeDescription = findViewById(R.id.dark_mode_description)

        privacyRow = findViewById(R.id.privacy_row)
        privacySwitch = findViewById(R.id.privacy_switch)
        privacyDescription = findViewById(R.id.privacy_description)
        privacyAdditionalSettings = findViewById(R.id.privacy_additional_settings)
        privacyFriendsSwitch = findViewById(R.id.privacy_friends_switch)
        privacyFriendsDescription = findViewById(R.id.privacy_friends_description)




        notificationRow.setOnClickListener {
            if (notificationDescription.isVisible) {
                slideUp(notificationDescription)
            } else {
                slideDown(notificationDescription)
            }
        }

        metricRow.setOnClickListener {
            if (metricDescription.isVisible) {
                slideUp(metricDescription)
            } else {
                slideDown(metricDescription)
            }
        }

        darkModeRow.setOnClickListener {
            if (darkModeDescription.isVisible) {
                slideUp(darkModeDescription)
            } else {
                slideDown(darkModeDescription)
            }
        }
        scheduledWorkoutsRow = findViewById(R.id.scheduled_workouts_row)
        scheduledWorkoutsTitle = findViewById(R.id.scheduled_workouts_title)


        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val isDarkModeSwitchLocked = sharedPreferences.getBoolean("isDarkModeSwitchLocked", false)
        darkModeSwitch.isEnabled = !isDarkModeSwitchLocked


        metricSwitch.setOnCheckedChangeListener { _, isChecked ->
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val userSettings = hashMapOf("metric" to isChecked)
                db.collection("users").document(userId).collection("settings")
                    .document("userSettings")
                    .set(userSettings, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("SettingsActivity", "Successfully updated metric in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w("SettingsActivity", "Error updating metric in Firestore", e)
                    }
            }
        }


        scheduledWorkoutsRow.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val selectedDaysCollection =
                    db.collection("users").document(userId).collection("selectedDays")
                val selectedHoursCollection =
                    db.collection("users").document(userId).collection("selectedHours")

                selectedDaysCollection.get().addOnSuccessListener { daysSnapshot ->
                    selectedHoursCollection.get().addOnSuccessListener { hoursSnapshot ->
                        val intent = if (!daysSnapshot.isEmpty || !hoursSnapshot.isEmpty) {
                            // Jeżeli istnieją dokumenty w kolekcjach selectedDays lub selectedHours, kieruj użytkownika do ScheduleWorkoutActivity2
                            Intent(this, ScheduleWorkoutActivity2::class.java)
                        } else {
                            // W przeciwnym razie kieruj użytkownika do ScheduleWorkoutActivity
                            Intent(this, ScheduleWorkoutActivity::class.java)
                        }
                        startActivity(intent)
                    }
                }
            } else {
                Log.e("SettingsActivity", "User ID is null")
            }
        }


        // Fetch user settings from Firebase and update the switches
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users")
                .document(userId)
                .collection("settings")
                .document("userSettings")
                .get()
                .addOnSuccessListener { document ->
                    val userSettings = document.data
                    if (userSettings != null) {
                        // Pobierz wartość firstSettingsChange z Firebase
                        firstSettingsChange = userSettings["firstSettingsChange"] as Boolean? ?: false
                        isDarkModeEnabled = userSettings["darkMode"] as Boolean? ?: false

                        // Check if notifications are enabled
                        val areNotificationsEnabled =
                            userSettings["notifications"] as Boolean? ?: false
                        notificationSwitch.isChecked = areNotificationsEnabled

                        // Check if metric is enabled
                        val isMetricEnabled = userSettings["metric"] as Boolean? ?: false
                        metricSwitch.isChecked = isMetricEnabled

                        // Usuń OnCheckedChangeListener
                        darkModeSwitch.setOnCheckedChangeListener(null)

                        // Ustaw wartość isChecked bez wywoływania OnCheckedChangeListener
                        darkModeSwitch.isChecked = isDarkModeEnabled

                        // Dodaj OnCheckedChangeListener z powrotem
                        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                            val userId = auth.currentUser?.uid

                            // Zapisz stan przełącznika do Firebase
                            if (userId != null) {
                                val userSettings = hashMapOf("darkMode" to isChecked)
                                db.collection("users").document(userId).collection("settings")
                                    .document("userSettings")
                                    .set(userSettings, SetOptions.merge())
                                    .addOnSuccessListener {
                                        Log.d("SettingsActivity", "Successfully updated darkMode in Firestore")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("SettingsActivity", "Error updating darkMode in Firestore", e)
                                    }
                            }

                            // Sprawdź, czy to pierwsza zmiana ustawień
                            if (firstSettingsChange) {
                                Toast.makeText(this, "Tryb zostanie zaaktualizowany po zrestartowaniu aplikacji", Toast.LENGTH_SHORT).show()

                                // Zablokuj interakcję z przełącznikiem
                                darkModeSwitch.isEnabled = false

                                // Zapisz stan blokady w SharedPreferences
                                sharedPreferences.edit().putBoolean("isDarkModeSwitchLocked", true).apply()
                            } else {
                                // Zmień tryb ciemny natychmiast, tylko jeśli firstSettingsChange jest false
                                if (!firstSettingsChange) {
                                    // Opóźnij zmianę trybu nocnego o 0,5 sekundy
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        AppCompatDelegate.setDefaultNightMode(
                                            if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                                            else AppCompatDelegate.MODE_NIGHT_NO
                                        )
                                    }, 100)
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to load settings: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Failed to load settings: User not logged in", Toast.LENGTH_SHORT).show()
        }

// Dodaj OnClickListener do wyświetlania komunikatu, gdy przełącznik jest zablokowany
        darkModeSwitch.setOnClickListener {
            if (isSwitchLocked) {
                Toast.makeText(
                    this,
                    "Proszę zrestartować aplikację, aby zmienić tryb",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Ustawienie początkowej widoczności dla dodatkowych ustawień prywatności
        privacyAdditionalSettings.visibility = if (privacySwitch.isChecked) View.VISIBLE else View.GONE

        // Zdarzenie zmiany stanu przełącznika prywatności
        privacySwitch.setOnCheckedChangeListener { _, isChecked ->
            // Aktualizacja widoczności dodatkowych ustawień prywatności
            privacyAdditionalSettings.visibility = if (isChecked) View.VISIBLE else View.GONE

            // Aktualizacja ustawień prywatności w bazie danych
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val userSettings = hashMapOf("privacy" to isChecked)
                db.collection("users").document(userId).collection("settings")
                    .document("userSettings")
                    .set(userSettings, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("SettingsActivity", "Successfully updated privacy in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w("SettingsActivity", "Error updating privacy in Firestore", e)
                    }
            }
        }

        // Zdarzenie zmiany stanu przełącznika prywatności dla znajomych
        privacyFriendsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Aktualizacja ustawień prywatności dla znajomych w bazie danych
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val userSettings = hashMapOf("privacyFriends" to isChecked)
                db.collection("users").document(userId).collection("settings")
                    .document("userSettings")
                    .set(userSettings, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("SettingsActivity", "Successfully updated privacyFriends in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w("SettingsActivity", "Error updating privacyFriends in Firestore", e)
                    }
            }
        }
   }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isRecreating", isRecreating)
    }


    override fun onStart() {
        super.onStart()
        Log.d("SettingsActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("SettingsActivity", "onResume")

        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val isDarkModeSwitchLocked = sharedPreferences.getBoolean("isDarkModeSwitchLocked", false)
        darkModeSwitch.isEnabled = !isDarkModeSwitchLocked

        // Fetch user settings from Firebase and update the switches
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users")
                .document(userId)
                .collection("settings")
                .document("userSettings")
                .get()
                .addOnSuccessListener { document ->
                    val userSettings = document.data
                    if (userSettings != null) {
                        // Check if privacy is enabled
                        val isPrivacyEnabled = userSettings["privacy"] as Boolean? ?: false
                        privacySwitch.isChecked = isPrivacyEnabled

                        // Check if privacy for friends is enabled
                        val isPrivacyFriendsEnabled = userSettings["privacyFriends"] as Boolean? ?: false
                        privacyFriendsSwitch.isChecked = isPrivacyFriendsEnabled

                        // Update visibility of the additional privacy settings
                        privacyAdditionalSettings.visibility = if (isPrivacyEnabled) View.VISIBLE else View.GONE
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to load settings: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Failed to load settings: User not logged in", Toast.LENGTH_SHORT).show()
        }




        isInSettingsActivity = true

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            val userId = auth.currentUser?.uid

            // Logika z onResume()
            if (isChecked) {
                requestExactAlarmPermission()

                // SCHEDULE_EXACT_ALARM permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val method = AlarmManager::class.java.getMethod("canScheduleExactAlarms")
                    val canScheduleExactAlarms = method.invoke(alarmManager) as Boolean

                    if (!canScheduleExactAlarms) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivityForResult(intent, EXACT_ALARM_REQUEST_CODE)
                    }
                }

                // FOREGROUND_SERVICE permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(
                            arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                            FOREGROUND_SERVICE_REQUEST_CODE
                        )
                    }
                }

                val areNotificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
                if (!areNotificationsEnabled) {
                    // Wyświetl dialog
                    AlertDialog.Builder(this)
                        .setTitle("Włącz powiadomienia")
                        .setMessage("Powiadomienia są wyłączone. Czy chcesz je włączyć w ustawieniach?")
                        .setPositiveButton("Tak") { _, _ ->
                            // Otwórz ustawienia powiadomień dla Twojej aplikacji
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            }
                            startActivity(intent)
                        }
                        .setNegativeButton("Nie", null)
                        .show()
                }
            }

            if (!isChecked) {
                val areNotificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
                if (areNotificationsEnabled) {
                    // Wyświetl dialog z prośbą o wyłączenie powiadomień w ustawieniach
                    AlertDialog.Builder(this)
                        .setTitle("Wyłącz powiadomienia")
                        .setMessage("Powiadomienia są włączone. Czy chcesz je wyłączyć w ustawieniach?")
                        .setPositiveButton("Tak") { _, _ ->
                            // Otwórz ustawienia powiadomień dla Twojej aplikacji
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            }
                            startActivity(intent)
                        }
                        .setNegativeButton("Nie", null)
                        .show()
                }
            }
            // Logika zapisywania w Firebase
            if (userId != null) {
                db.collection("users").document(userId).collection("settings")
                    .document("userSettings")
                    .update("notifications", isChecked)
                    .addOnSuccessListener {
                        Log.d("SettingsActivity", "Successfully updated notifications in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w("SettingsActivity", "Error updating notifications in Firestore", e)
                    }
            }
        }



        nextButton.setOnClickListener {
            val userId = auth.currentUser?.uid

            if (userId != null) {
                // Sprawdź, czy dokumenty selectedDays i selectedHours istnieją
                val selectedDaysCollection = db.collection("users").document(userId)
                    .collection("selectedDays")
                val selectedHoursCollection = db.collection("users").document(userId)
                    .collection("selectedHours")

                selectedDaysCollection.get().addOnSuccessListener { daysSnapshot ->
                    selectedHoursCollection.get().addOnSuccessListener { hoursSnapshot ->
                        val hasSelectedDays = daysSnapshot.documents.any { it.id != "schedule" }
                        val hasSelectedHours = hoursSnapshot.documents.any { it.id != "schedule" }
                        val hasScheduleDays = daysSnapshot.documents.any { it.id == "schedule" }
                        val hasScheduleHours = hoursSnapshot.documents.any { it.id == "schedule" }

                        val intent = when {
                            // Jeśli są wygenerowane dokumenty i firstSettingsChange jest true, przejdź do SuccessActivity2
                            hasSelectedDays && hasSelectedHours && firstSettingsChange -> {
                                Intent(this, SuccessActivity2::class.java)
                            }
                            // Jeśli są wygenerowane dokumenty i firstSettingsChange jest false, przejdź do DashboardActivity
                            hasSelectedDays && hasSelectedHours && !firstSettingsChange -> {
                                Intent(this, DashboardActivity::class.java)
                            }
                            // Jeśli w dokumentach jest schedule, przejdź do DashboardActivity
                            hasScheduleDays && hasScheduleHours -> {
                                Intent(this, DashboardActivity::class.java)
                            }
                            // W przeciwnym razie wykonaj animację potrząsania na przycisku do ScheduleWorkoutActivity i wyświetl komunikat
                            else -> {
                                // Odwołanie się do RelativeLayout wewnątrz NeumorphCardView za pomocą identyfikatora
                                val scheduledWorkoutsContainer =
                                    findViewById<FrameLayout>(R.id.scheduled_workouts_container)

                                // Zastosowanie animacji potrząsania do przycisku
                                shakeView(scheduledWorkoutsContainer)

                                // Wyświetlenie komunikatu
                                Toast.makeText(
                                    this,
                                    "Proszę ustawić dni treningowe",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@addOnSuccessListener
                            }


                        }

                        // Zapisz ustawienia i przejdź do odpowiedniej aktywności
                        settings["notifications"] = notificationSwitch.isChecked
                        settings["metric"] = metricSwitch.isChecked
                        settings["darkMode"] = darkModeSwitch.isChecked
                        settings["privacy"] = privacySwitch.isChecked // Add privacy setting
                        settings["privacyFriends"] = privacyFriendsSwitch.isChecked // Add privacy for friends setting
                        settings["firstSettingsChange"] = false

                        db.collection("users")
                            .document(userId)
                            .collection("settings")
                            .document("userSettings")
                            .set(settings, SetOptions.merge())
                            .addOnSuccessListener {
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(
                                    this,
                                    "Błąd: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Failed to save settings: User not logged in",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



    override fun onPause() {
        super.onPause()
        Log.d("SettingsActivity", "onPause")
        isInSettingsActivity = false
    }

    override fun onStop() {
        super.onStop()
        Log.d("SettingsActivity", "onStop")
        val userId = auth.currentUser?.uid
        if (userId != null && darkModeSettingChanged) {
            db.collection("users")
                .document(userId)
                .collection("settings")
                .document("userSettings")
                .update("darkMode", isDarkModeEnabled)
            darkModeSettingChanged = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SettingsActivity", "onDestroy")

    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission =
                checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }
    }


    private fun slideDown(v: TextView) {
        if (!v.isVisible) {
            v.visibility = View.VISIBLE
            v.alpha = 0.0f

            // Start the animation
            v.animate()
                .setDuration(500)
                .translationY(0f)
                .alpha(1.0f)
                .start()
        }
    }


    private fun slideUp(v: TextView) {
        if (v.isVisible) {
            v.animate()
                .setDuration(500)
                .translationY(-v.height.toFloat())
                .alpha(0.0f)
                .withEndAction {
                    v.visibility = View.GONE
                    v.translationY = 0f
                }
                .start()
        }
    }

    private fun shakeView(view: View) {
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
        view.startAnimation(shake)
    }
}
