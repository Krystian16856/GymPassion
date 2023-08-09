package com.example.gympassion

import android.animation.Animator
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TableLayout
import androidx.core.content.res.ResourcesCompat
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import soup.neumorphism.NeumorphButton
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.math.floor

class DayWorkoutActivity : AppCompatActivity() {
    private lateinit var exercisesContainer: TableLayout

    private lateinit var trainButton: ImageView
    private lateinit var playPauseButton: ImageView
    private lateinit var animationView: LottieAnimationView
    private lateinit var timerTextView: TextView
    private var timerPaused = false
    private lateinit var saveButton: ImageView
    private var timer: Timer? = null
    private var startTime = 0L
    private var pauseTime = 0L
    private var elapsedTime = 0L
    private var workoutEndTime: Timestamp? = null
    private var workoutDuration: Long = 0
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DayWorkoutActivity", "onCreate called")
        setContentView(R.layout.activity_day_workout)

        exercisesContainer = findViewById(R.id.exercisesContainer)

        val dayTitle = intent.getStringExtra("dayTitle") ?: "Default Title"
        title = dayTitle


        trainButton = findViewById(R.id.gym_time_button)
        trainButton.isEnabled = true
        saveButton = findViewById(R.id.save_button)
        playPauseButton = findViewById(R.id.play_pause_button)
        animationView = findViewById(R.id.animation_view)
        timerTextView = findViewById(R.id.timer_text_view)
        timerTextView.visibility = View.GONE
        animationView.setBackgroundColor(Color.TRANSPARENT)
        workoutEndTime = Timestamp.now()

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        val dayId = intent.getStringExtra("dayId")
        if (dayId != null) {
            // Extract dayIndex from dayId
            val dayIndex = dayId.split("_")[0].toInt()

            loadExercisesForDay(dayId, dayIndex)
        } else {
            Log.e("DayWorkoutActivity", "No dayId passed in Intent")
        }

        var workoutStartTime: Timestamp? = null
        var workoutStartMillis: Long = 0
        var isSaving = false


        val increaseAnimation = AnimationUtils.loadAnimation(this, R.anim.increase_size)
        val decreaseAnimation = AnimationUtils.loadAnimation(this, R.anim.decrease_size)

        saveButton.isEnabled = false



        trainButton.setOnClickListener {
            // disable the button immediately after it is clicked
            it.isEnabled = false
            animationView.setAnimation("321.json")
            animationView.playAnimation()
            animationView.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    startTimer()
                    timerTextView.visibility = View.VISIBLE
                    timerTextView.alpha = 0f // initially transparent
                    timerTextView.animate()
                        .alpha(1f) // animate to fully opaque
                        .setDuration(1000) // over the course of 1 second
                        .setListener(null)
                    // Register workout start time when animation ends
                    workoutStartTime = Timestamp.now()
                    workoutStartMillis = System.currentTimeMillis()

                    for (i in 0 until exercisesContainer.childCount) {
                        Log.d("DayWorkoutActivity", "In the for loop, iteration: $i")
                        val child = exercisesContainer.getChildAt(i)
                        if (child is EditText) {
                            child.isEnabled = true
                        }
                    }

                    saveButton.isEnabled=true
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            timerTextView.startAnimation(increaseAnimation)
            timerTextView.setTextColor(Color.GREEN)
        }




        saveButton.setOnClickListener {
            Log.d("DayWorkoutActivity", "Save button clicked")
            val wasRunning = !timerPaused
            if (wasRunning) {
                stopTimer()
            }
            isSaving = true
            // Pobieramy wszystkie TextView (nazwy ćwiczeń) na podstawie tagu
            val exerciseNames = mutableListOf<String>()
            for (i in 0 until exercisesContainer.childCount) {
                Log.d("DayWorkoutActivity", "In the for loop, iteration: $i")
                val child = exercisesContainer.getChildAt(i)
                Log.d("DayWorkoutActivity", "Child class: ${child.javaClass}")
                val tag = child.tag
                Log.d("DayWorkoutActivity", "Child tag: $tag")
                if (child is ViewGroup && tag is String) {
                    Log.d("DayWorkoutActivity", "Found TextView with tag: $tag")
                    exerciseNames.add(tag)
                } else if (child is ViewGroup) {
                    for (j in 0 until child.childCount) {
                        val grandChild = child.getChildAt(j)
                        val grandChildTag = grandChild.tag
                        if (grandChild is TextView && grandChildTag is String && !grandChildTag.contains("repetitions") && !grandChildTag.contains("weight")) {
                            Log.d("DayWorkoutActivity", "Found TextView in child ViewGroup with tag: $grandChildTag")
                            exerciseNames.add(grandChildTag)
                        }
                    }
                }

            }
            Log.d("DayWorkoutActivity", "Exercise names: $exerciseNames")
            Log.d("DayWorkoutActivity", "About to map exerciseNames")
            // Dla każdej nazwy ćwiczenia, pobieramy EditText (dla powtórzeń i wagi) na podstawie tagu
            val exerciseData = exerciseNames.map { name ->
                val repetitionViews = mutableListOf<EditText>()
                val weightViews = mutableListOf<EditText>()
                Log.d("DayWorkoutActivity", "About to iterate over children of exercisesContainer")
                for (i in 0 until exercisesContainer.childCount) {
                    Log.d("DayWorkoutActivity", "In the for loop, iteration: $i")
                    val child = exercisesContainer.getChildAt(i)
                    Log.d("DayWorkoutActivity", "Child class: ${child.javaClass}")
                    val tag = child.tag
                    Log.d("DayWorkoutActivity", "Child tag: $tag")
                    if (child is EditText && tag is String) {
                        Log.d("DayWorkoutActivity", "Found EditText with tag: $tag")
                        if (tag.startsWith("$name-repetitions-")) {
                            repetitionViews.add(child)
                        } else if (tag.startsWith("$name-weight-")) {
                            weightViews.add(child)
                        }
                    } else if (child is ViewGroup) {
                        Log.d("DayWorkoutActivity", "Found ViewGroup")
                        for (j in 0 until child.childCount) {
                            val grandChild = child.getChildAt(j)
                            val grandChildTag = grandChild.tag
                            if (grandChild is EditText && grandChildTag is String) {
                                Log.d(
                                    "DayWorkoutActivity",
                                    "Found EditText in child ViewGroup with tag: $grandChildTag"
                                )
                                if (grandChildTag.startsWith("$name-repetitions-")) {
                                    repetitionViews.add(grandChild)
                                } else if (grandChildTag.startsWith("$name-weight-")) {
                                    weightViews.add(grandChild)
                                }
                            }
                        }
                    }
                }

                Log.d(
                    "DayWorkoutActivity",
                    "Found ${repetitionViews.size} repetition EditTexts and ${weightViews.size} weight EditTexts"
                )
                val repetitions = repetitionViews.map { it.text.toString().toDoubleOrNull() ?: 0.0 }
                val weights = weightViews.map { it.text.toString().toDoubleOrNull() ?: 0.0 }
                val series = repetitionViews.size


                // Zwracamy nazwę ćwiczenia, powtórzenia, wagi i serie jako parę
                name to ExerciseData(repetitions, weights, series)

            }
            Log.d("DayWorkoutActivity", "Finished mapping exerciseNames")


            workoutDuration = elapsedTime / 1000
            Log.d("DayWorkoutActivity", "Workout duration: $workoutDuration seconds")


            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val db = FirebaseFirestore.getInstance()

            if (userId != null) {
                Log.d("DayWorkoutActivity", "User ID: $userId")
                val dayId = intent.getStringExtra("dayId") ?: return@setOnClickListener

                Log.d("DayWorkoutActivity", "Day ID: $dayId")

                // Get the document reference for the specific dayId in the workoutResults collection
                val dayDocument = db.collection("users").document(userId)
                    .collection("workoutResults").document(dayId)




                Log.d("DayWorkoutActivity", "Exercise data: $exerciseData")
                Log.d("DayWorkoutActivity", "About to iterate over exerciseData. Size: ${exerciseData.size}")

                // For each exercise, create a new document in the exercises subcollection of the dayId document
                exerciseData.forEach { (name, data) ->
                    Log.d("DayWorkoutActivity", "Processing exercise: $name")
                    Log.d("DayWorkoutActivity", "Saving exercise data: $name - ${data.repetitions} - ${data.weights} - ${data.series}")

                    val exerciseResult = mapOf(
                        "dayId" to dayId,
                        "exerciseName" to name,
                        "repetitions" to data.repetitions,
                        "weights" to data.weights,
                        "series" to data.series,
                        "previousWeekWeight" to data.repetitions.zip(data.weights) { reps, weight -> "$reps x $weight" },
                        "timestamp" to workoutEndTime, // The timestamp will now be a field in the document
                        "duration" to workoutDuration // Add workout duration to each exercise
                    )
                    // Tworzymy unikalną nazwę dla dokumentu
                    val uniqueDocumentName = "${name}-${Timestamp.now().toString()}"
                    Log.d("DayWorkoutActivity", "About to save exercise data to Firebase. Document name: $uniqueDocumentName")
                    // Dodajemy nowy dokument do kolekcji "exercises" z unikalną nazwą
                    dayDocument.collection("exercises").document(uniqueDocumentName)
                        .set(exerciseResult)
                        .addOnSuccessListener { Log.d("DayWorkoutActivity", "Successfully saved exercise data for $name") }
                        .addOnFailureListener { e -> Log.e("DayWorkoutActivity", "Failed to save exercise data for $name", e) }
                    Log.d("DayWorkoutActivity", "Exercise data saved for $name")

                }
            } else {
                Log.e("DayWorkoutActivity", "User ID is null")
            }
            pauseTimer()
            isSaving = false

        animationView.visibility = View.VISIBLE
        animationView.setAnimation("success.json")
        animationView.playAnimation()
        animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                // Po zakończeniu animacji, przenieś użytkownika do DashboardActivity
                val intent = Intent(this@DayWorkoutActivity, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
    }


        playPauseButton.setOnClickListener {
            if (timerPaused) {
                Log.d("DayWorkoutActivity", "Resuming timer. Current elapsedTime: $elapsedTime")
                resumeTimer()
                timerTextView.startAnimation(increaseAnimation)
                timerTextView.setTextColor(Color.GREEN)
                Log.d("DayWorkoutActivity", "Recorded startTime: $startTime")
            } else {
                Log.d("DayWorkoutActivity", "Pausing timer. Current elapsedTime: $elapsedTime")
                pauseTimer()
                timerTextView.startAnimation(decreaseAnimation)
                timerTextView.setTextColor(Color.RED)
                Log.d("DayWorkoutActivity", "Updated elapsedTime: $elapsedTime")
            }
            timerPaused = !timerPaused
        }

        Log.d("DayWorkoutActivity", "exercisesContainer child count: ${exercisesContainer.childCount}")


        exercisesContainer = findViewById(R.id.exercisesContainer)
        Log.d(
            "DayWorkoutActivity",
            "exercisesContainer found with child count: ${exercisesContainer.childCount}"
        )




    }

    override fun onBackPressed() {
        if (timerTextView.visibility == View.VISIBLE) {
            AlertDialog.Builder(this)
                .setTitle("Czy na pewno chcesz wyjść?")
                .setMessage("Dane nie zostaną zapisane.")
                .setPositiveButton("Tak") { dialog, _ ->
                    super.onBackPressed()
                    dialog.dismiss()
                }
                .setNegativeButton("Nie") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            super.onBackPressed()
        }
    }


    private fun stopTimer() {
        timer?.cancel()
        timer = null
        val currentTime = System.currentTimeMillis()
        elapsedTime += currentTime - startTime
    }

    private fun pauseTimer() {
        stopTimer()
    }



    private fun resumeTimer() {
        startTime = System.currentTimeMillis()
        startTimer()
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val totalElapsedTime = elapsedTime + (currentTime - startTime)
                val secondsElapsed = totalElapsedTime / 1000
                val minutes = secondsElapsed / 60
                val seconds = secondsElapsed % 60
                runOnUiThread {
                    timerTextView.text = String.format("%02d:%02d", minutes, seconds)
                }
            }
        }, 0, 1000)
    }

    private fun showExerciseMenu(name: String, txtFile: String?, mp4File: String?) {
        val builder = AlertDialog.Builder(this)
        val storage = FirebaseStorage.getInstance()
        builder.setTitle(name)
            .setItems(arrayOf("Instrukcja krok po kroku", "Film prezentujący ćwiczenie"), DialogInterface.OnClickListener { _, which ->
                when (which) {
                    0 -> {
                        // Pobieranie i wyświetlanie pliku tekstowego
                        if (txtFile != null) {
                            val storageRef = storage.reference.child("exercisesFiles2/$txtFile")
                            storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                                val description = String(bytes)
                                val alertDialog = AlertDialog.Builder(this)
                                    .setTitle(name)
                                    .setMessage(description)
                                    .setPositiveButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .create()
                                alertDialog.show()
                            }.addOnFailureListener { exception ->
                                Log.d("DayWorkoutActivity", "Failed to download text file: $exception")
                            }
                        } else {
                            Toast.makeText(this, "Brak pliku tekstowego dla tego ćwiczenia.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        // Pobieranie i wyświetlanie pliku wideo
                        if (mp4File != null) {
                            val storageRef = storage.reference.child("exercisesFiles2/$mp4File")
                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                val intent = Intent(this, VideoViewActivity::class.java)
                                intent.putExtra("videoUrl", uri.toString())
                                intent.putExtra("exerciseName", name)
                                Log.d(
                                    "DayWorkoutActivity",
                                    "Starting VideoViewActivity with videoUrl: ${uri.toString()}"
                                )
                                startActivity(intent)
                            }.addOnFailureListener { exception ->
                                Log.d("DayWorkoutActivity", "Failed to download video file: $exception")
                            }
                        } else {
                            Toast.makeText(this, "Brak pliku wideo dla tego ćwiczenia.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        builder.create().show()
    }




    private fun loadPreviousWorkoutResultsForExercise(userId: String, exerciseName: String, setIndex: Int, exerciseRow: ViewGroup, dayId: String) {
        val db = FirebaseFirestore.getInstance()

        val fixedDayId = dayId

        Log.d("Firestore", "Starting to load previous workout results...")
        Log.d("Firestore", "FixedDayId: $fixedDayId")

        db.collection("users").document(userId)
            .collection("workoutResults").document(fixedDayId)
            .collection("exercises")
            .whereEqualTo("exerciseName", exerciseName)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val document = documents.documents.firstOrNull()
                Log.d("Firestore", "Document: $document")
                val previousWeekWeight = document?.get("previousWeekWeight") as? List<String>

                val prevWeekResultTextView: TextView = exerciseRow.findViewById(R.id.prev_week_result)

                if (previousWeekWeight != null && previousWeekWeight.isNotEmpty()) {
                    val prevWeekResult = previousWeekWeight.getOrNull(setIndex - 1)
                    prevWeekResultTextView.text = prevWeekResult ?: "0 x 0"
                    Log.d("Firestore", "Got previousWeekWeight: $previousWeekWeight")
                    Log.d("Firestore", "Got result for set $setIndex: $prevWeekResult")
                    Log.d("Firestore", "Updated text view with result: $prevWeekResult")
                } else {
                    prevWeekResultTextView.text = "Brak danych"
                    Log.d("Firestore", "previousWeekWeight is null or empty.")
                }
            }
            .addOnFailureListener { exception ->
                Log.d("Firestore", "Failed to get workout results", exception)
            }
    }



    private fun loadExercisesForDay(dayId: String, dayIndex: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        if (userId != null) {
            val fixedDayId = "${dayId}"

            val dayDocument = db.collection("users").document(userId)
                .collection("workoutPlans").document(fixedDayId)

            dayDocument.get()
                .addOnSuccessListener { document ->
                    val title = document.getString("title") ?: "Default Title"
                    val exercisesData = document.get("exercises") as? List<Map<String, Any>>


                    Log.d("DayWorkoutActivity", "User: $userId")
                    Log.d("DayWorkoutActivity", "Title: $title")
                    Log.d("DayWorkoutActivity", "ExercisesData: $exercisesData")
                    Log.d("DayWorkoutActivity", "Loading exercises for fixedDayId: $fixedDayId")

                    // Check if exercisesData list is null or empty
                    if (exercisesData == null || exercisesData.isEmpty()) {
                        // Show message when no data
                        Toast.makeText(this, "Brak danych. Proszę uzupełnić plan treningowy.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    exercisesData?.mapNotNull { exerciseData ->
                        val name = exerciseData["name"] as? String
                        val repetitions = exerciseData["repetitions"]?.toString()?.toLongOrNull()
                        val sets = exerciseData["sets"]?.toString()?.toLongOrNull()

                        if (name != null && repetitions != null && sets != null) {
                            val exerciseNameTextView = TextView(this).apply {
                                tag = name
                                text = name
                                textSize = 25f
                                gravity = Gravity.CENTER_HORIZONTAL
                                setPadding(0, 10.dpToPx(), 0, 10.dpToPx())
                                typeface = Typeface.DEFAULT_BOLD

                                // Ustawienie tła i koloru tekstu w zależności od trybu
                                if (isNightMode()) {
                                    setBackgroundColor(Color.WHITE)
                                    setTextColor(Color.BLACK)
                                } else {
                                    setBackgroundColor(Color.BLACK)
                                    setTextColor(Color.WHITE)
                                }
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
                            }
                            val menuButton = ImageButton(this).apply {
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.1f)
                                setImageResource(R.drawable.ic_menu) // musisz dostarczyć odpowiednią ikonę
                                background = null
                                setPadding(0, 0, 0, 0)
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                setOnClickListener {
                                    showExerciseMenu(name, exerciseData["txtFile"] as? String, exerciseData["mp4File"] as? String)
                                }
                                // Ustawienie tła w zależności od trybu
                                if (isNightMode()) {
                                    setBackgroundColor(Color.WHITE)
                                } else {
                                    setBackgroundColor(Color.BLACK)
                                }
                            }

                            val exerciseHeaderLayout = LinearLayout(this).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER_VERTICAL
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                addView(exerciseNameTextView)
                                addView(menuButton)
                            }


                            exercisesContainer.addView(exerciseHeaderLayout)


                            val inflater = LayoutInflater.from(this)
                            val exerciseHeaderRow = inflater.inflate(
                                R.layout.exercise_header_row,
                                exercisesContainer,
                                false
                            ) as ViewGroup

                            // Ustawienie tła i koloru tekstu w zależności od trybu
                            if (isNightMode()) {
                                exerciseHeaderRow.setBackgroundColor(Color.DKGRAY)
                                for (i in 0 until exerciseHeaderRow.childCount) {
                                    val child = exerciseHeaderRow.getChildAt(i)
                                    if (child is TextView) {
                                        child.setTextColor(Color.WHITE)
                                    }
                                }
                            } else {
                                exerciseHeaderRow.setBackgroundColor(Color.LTGRAY)
                                for (i in 0 until exerciseHeaderRow.childCount) {
                                    val child = exerciseHeaderRow.getChildAt(i)
                                    if (child is TextView) {
                                        child.setTextColor(Color.BLACK)
                                    }
                                }
                            }


                            // Aktualizacja wartości TextView dla 'metric' w nagłówku
                            updateMetricTextView(exerciseHeaderRow, userId)

                            exercisesContainer.addView(exerciseHeaderRow)

                            for (i in 1..sets.toInt()) {
                                val exerciseRow = inflater.inflate(
                                    R.layout.exercise_row,
                                    exercisesContainer,
                                    false
                                ) as ViewGroup

                                // Aktualizacja wartości TextView dla 'metric' w każdym wierszu
                                updateMetricTextView(exerciseRow, userId)

                                val setNumberTextView: TextView =
                                    exerciseRow.findViewById(R.id.set_number)
                                setNumberTextView.text = i.toString()


                                // Wywołanie funkcji loadPreviousWorkoutResultsForExercise z exerciseRow jako argumentem
                                // Używamy i jako indeksu serii, zamiast stałej wartości sets
                                loadPreviousWorkoutResultsForExercise(
                                    userId,
                                    name,
                                    i,
                                    exerciseRow,
                                    fixedDayId
                                )

                                val currentRepetitionsSetsEditText: EditText =
                                    exerciseRow.findViewById<EditText>(R.id.current_repetitions_sets).apply {
                                        tag = "$name-repetitions-$i" // dodajemy tag do EditText
                                        Log.d("DayWorkoutActivity", "Tag set for repetitions EditText: $tag")
                                        isEnabled = false // zablokuj pole tekstowe na początku

                                        // Dodajemy AnimatorListener do animacji
                                        animationView.addAnimatorListener(object : Animator.AnimatorListener {
                                            override fun onAnimationStart(animation: Animator) {}

                                            override fun onAnimationEnd(animation: Animator) {
                                                isEnabled = true // odblokuj pole tekstowe, gdy animacja się kończy
                                            }

                                            override fun onAnimationCancel(animation: Animator) {}

                                            override fun onAnimationRepeat(animation: Animator) {}
                                        })
                                    }

                                currentRepetitionsSetsEditText.setText(repetitions.toString())

                                val currentWeightEditText: EditText =
                                    exerciseRow.findViewById<EditText>(R.id.current_weight).apply {
                                        tag = "$name-weight-$i" // dodajemy tag do EditText
                                        Log.d(
                                            "DayWorkoutActivity",
                                            "Tag set for weight EditText: $tag"
                                        )
                                        isEnabled = false  // Ustawienie pola na zablokowane

                                        // Dodajemy AnimatorListener do animacji
                                        animationView.addAnimatorListener(object : Animator.AnimatorListener {
                                            override fun onAnimationStart(animation: Animator) {}

                                            override fun onAnimationEnd(animation: Animator) {
                                                isEnabled = true // odblokuj pole tekstowe, gdy animacja się kończy
                                            }

                                            override fun onAnimationCancel(animation: Animator) {}

                                            override fun onAnimationRepeat(animation: Animator) {}
                                        })
                                    }
                                currentWeightEditText.setText("0")

                                currentWeightEditText.isEnabled = false

                                exercisesContainer.addView(exerciseRow)


                                val divider =
                                    inflater.inflate(R.layout.divider, exercisesContainer, false)
                                exercisesContainer.addView(divider)
                            }

                            val bigDivider =
                                inflater.inflate(R.layout.big_divider, exercisesContainer, false)
                            exercisesContainer.addView(bigDivider)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("DayWorkoutActivity", "Error loading exercises", exception)
                    Log.w("DayWorkoutActivity", "Exception message: ${exception.message}")
                }
        } else {
            Log.e("DayWorkoutActivity", "User ID is null")
        }
    }

    private fun updateMetricTextView(view: View, userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).collection("settings").document("userSettings").get()
            .addOnSuccessListener { document ->
                val isMetric = document.getBoolean("metric") ?: false
                val metricTextValue = if (isMetric) "Kg" else "Lb"

                val metricTextView: TextView? = view.findViewById(R.id.metricTextView) // Zmień na właściwe ID
                metricTextView?.text = metricTextValue
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Błąd: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    fun isNightMode(): Boolean {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }


    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
}


