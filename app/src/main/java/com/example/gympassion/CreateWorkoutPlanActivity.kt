package com.example.gympassion

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gympassion.CreateWorkoutPlanActivity.Companion.REQUEST_CODE
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import soup.neumorphism.NeumorphButton
import com.example.gympassion.OnDayClickListener
import com.google.firebase.auth.FirebaseAuth


class CreateWorkoutPlanActivity : AppCompatActivity(), OnDayClickListener {
    private lateinit var daysAdapter: DaysAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Kreowanie planu"

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        auth.currentUser?.let { currentUser ->
            db.collection("users").document(currentUser.uid).get().addOnSuccessListener { document ->
                if (document != null) {
                    val isDarkModeOn = document.getBoolean("darkMode")
                    if (isDarkModeOn != null && isDarkModeOn) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            }.addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
        }
        setContentView(R.layout.activity_create_workout_plan)

        val daysRecyclerView = findViewById<RecyclerView>(R.id.days_recycler_view)
        daysAdapter = DaysAdapter(this, R.layout.day, false)
        daysRecyclerView.adapter = daysAdapter
        daysRecyclerView.layoutManager = LinearLayoutManager(this)

        val initialDay =
            Day(title = "Dzień 1", exercises = mutableListOf(), exerciseIds = mutableListOf())
        daysAdapter.setDays(listOf(initialDay))

        val addButton = findViewById<NeumorphButton>(R.id.add_day_button)
        addButton.setOnClickListener {
            val newDay =
                Day(
                    title = "Dzień ${daysAdapter.itemCount + 1}",
                    exercises = mutableListOf(),
                    exerciseIds = mutableListOf()
                )
            daysAdapter.addDay(newDay)
            daysRecyclerView.layoutManager?.scrollToPosition(daysAdapter.itemCount - 1)
        }

        val saveButton = findViewById<NeumorphButton>(R.id.save_button)
        saveButton.setOnClickListener {
            saveWorkoutPlan()
        }
    }


    companion object {
        const val REQUEST_CODE = 1
    }

    override fun onDayDelete(day: Day) {
        AlertDialog.Builder(this)
            .setTitle("Usunięcie zestawu ćwiczeń")
            .setMessage("Czy na pewno chcesz usunąć ten zestaw ćwiczeń?")
            .setPositiveButton("Tak") { _, _ ->
                daysAdapter.removeDay(day)
                daysAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("Nie", null)
            .show()
    }

    override fun onDayTitleClick(day: Day) {
        // Create AlertDialog to input new day title
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Zmień tytuł treningu")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(day.title)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val newTitle = input.text.toString()
            day.title = newTitle
            daysAdapter.notifyDataSetChanged()
        }
        builder.setNegativeButton("Anuluj") { dialog, _ -> dialog.cancel() }

        builder.show()
    }



    override fun onDayClick(day: Day) {
        Log.d("CreateWorkoutPlan", "onDayClick: Day clicked: ${day.title}")
        if (day.title == null) {
            Log.e("CreateWorkoutPlan", "Day title is null")
        }
        val intent = Intent(this, SelectExerciseActivity::class.java)
        Log.d("CreateWorkoutPlanAct", "Sending day title: ${day.title}")
        intent.putExtra("Dzień", day.title)
        startActivityForResult(intent, REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            "CreateWorkoutPlan",
            "onActivityResult called with requestCode: $requestCode, resultCode: $resultCode"
        )
        Log.d("CreateWorkoutPlan", "onActivityResult: Received result from SelectExerciseActivity")

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedExercise = data?.getSerializableExtra("selectedExercise") as Exercise?
            val removedExercise = data?.getSerializableExtra("removedExercise") as Exercise?
            val dayTitle = data?.getStringExtra("Dzień")
            Log.d(
                "CreateWorkoutPlan",
                "onActivityResult: selectedExercise: ${selectedExercise?.name}, removedExercise: ${removedExercise?.name}, dayTitle: $dayTitle"
            )

            if (dayTitle != null) {
                val daysList = daysAdapter.getDays()
                for (day in daysList) {
                    if (day.title == dayTitle) {
                        if (selectedExercise != null) {
                            Log.d(
                                "CreateWorkoutPlan",
                                "Before add: Total exercises in the day: ${day.exercises.size}"
                            )
                            day.exercises.add(selectedExercise)
                            day.exerciseIds.add(selectedExercise.id)
                            Log.d(
                                "CreateWorkoutPlan",
                                "After add: Total exercises in the day: ${day.exercises.size}"
                            )
                            Log.d(
                                "CreateWorkoutPlan",
                                "onActivityResult: Added exercise: ${selectedExercise.name} to day: ${day.title}. Total exercises in the day: ${day.exercises.size}"
                            )
                        }
                        if (removedExercise != null) {
                            Log.d(
                                "CreateWorkoutPlan",
                                "Before remove: Total exercises in the day: ${day.exercises.size}"
                            )
                            val removeIndex =
                                day.exercises.indexOfFirst { it.id == removedExercise.id }
                            Log.d("CreateWorkoutPlan", "Index of removed exercise: $removeIndex")
                            if (removeIndex != -1) {
                                day.exercises.removeAt(removeIndex)
                                day.exerciseIds.removeAt(removeIndex)
                                Log.d(
                                    "CreateWorkoutPlan",
                                    "After remove: Total exercises in the day: ${day.exercises.size}"
                                )
                                Log.d(
                                    "CreateWorkoutPlan",
                                    "onActivityResult: Removed exercise: ${removedExercise.name} from day: ${day.title}. Total exercises in the day: ${day.exercises.size}"
                                )
                            }
                        }

                        daysAdapter.notifyDataSetChanged()
                        break
                    }
                }
            } else {
                Log.e("CreateWorkoutPlan", "dayTitle is null")
            }
        } else {
            Log.e(
                "CreateWorkoutPlan",
                "Received unexpected requestCode: $requestCode or resultCode: $resultCode"
            )
        }
    }

    fun saveWorkoutPlan() {
        val daysList = daysAdapter.getDays()
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            for (day in daysList) {
                val exercisesList = day.exercises
                val dayData = mutableMapOf<String, Any>()
                dayData["title"] = day.title
                dayData["exercises"] = exercisesList.map { exercise ->
                    mapOf(
                        "id" to exercise.id,
                        "name" to exercise.name,
                        "txtFile" to exercise.descriptionFile,
                        "mp4File" to exercise.videoFile,
                        "sets" to 0,
                        "repetitions" to 0,
                        "isAddedToDay" to exercise.isAddedToDay
                    )
                }
                db.collection("users").document(userId).collection("workoutPlans")
                    .document(day.dayId).set(dayData)
                    .addOnSuccessListener {
                        Log.d("CreateWorkoutPlan", "Workout day saved with title: ${day.title}")
                    }
                    .addOnFailureListener { e ->
                        Log.w("CreateWorkoutPlan", "Error adding workout day", e)
                    }
            }
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } else {
            Log.e("CreateWorkoutPlan", "User ID is null")
        }
    }
}