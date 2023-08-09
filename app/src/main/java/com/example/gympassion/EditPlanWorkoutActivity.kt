package com.example.gympassion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import soup.neumorphism.NeumorphButton
import com.example.gympassion.OnDayClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditPlanWorkoutActivity : AppCompatActivity(), OnDayClickListener {
    private lateinit var daysAdapter: DaysAdapter2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_plan_workout)

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        title = "Edycja planu"



        val daysRecyclerView = findViewById<RecyclerView>(R.id.days_recycler_view)
        daysAdapter = DaysAdapter2(this, R.layout.day2, true)
        daysRecyclerView.adapter = daysAdapter
        daysRecyclerView.layoutManager = LinearLayoutManager(this)

        loadWorkoutPlan()




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


    private fun loadWorkoutPlan() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            val workoutPlanCollection =
                db.collection("users").document(userId).collection("workoutPlans")
            workoutPlanCollection.get()
                .addOnSuccessListener { documents ->
                    val days = mutableListOf<Day>()
                    for (document in documents) {
                        val title = document.getString("title") ?: "Default Title"
                        val exercisesData = document.get("exercises") as? List<Map<String, Any>>
                        val exercises = exercisesData?.mapNotNull { exerciseData ->
                            Log.d("EditPlanWorkout", "Processing exercise data: $exerciseData")
                            val id = exerciseData["id"] as? String
                            val name = exerciseData["name"] as? String
                            val txtFile = exerciseData["txtFile"] as? String
                            val mp4File = exerciseData["mp4File"] as? String
                            val sets = exerciseData["sets"]?.toString()?.toIntOrNull()
                            val repetitions = exerciseData["repetitions"]?.toString()?.toIntOrNull()
                            Log.d("EditPlanWorkout", "Loaded sets: $sets, repetitions: $repetitions")
                            val isAddedToDay = exerciseData["isAddedToDay"] as? Boolean
                            Log.d("EditPlanWorkout", "Sets: $sets, Repetitions: $repetitions")
                            if (id != null && name != null && txtFile != null && mp4File != null && sets != null && repetitions != null) {
                                Exercise(
                                    id = id,
                                    name = name,
                                    descriptionFile = txtFile,
                                    videoFile = mp4File,
                                    sets = sets,
                                    repetitions = repetitions,
                                    isAddedToDay = isAddedToDay ?: false
                                )

                            } else {
                                Log.w("EditPlanWorkout", "Invalid exercise data: $exerciseData")
                                null
                            }
                        } ?: listOf()
                        days.add(
                            Day(
                                title = title,
                                exercises = exercises.toMutableList(),
                                exerciseIds = exercises.map { it.id }.toMutableList()
                            )
                        )
                    }
                    daysAdapter.setDays(days)
                    Log.d("EditPlanWorkout", "Data loaded from Firebase: $days")
                }
                .addOnFailureListener { exception ->
                    Log.w("EditPlanWorkout", "Error loading workout plan", exception)
                }
        } else {
            Log.e("EditPlanWorkout", "User ID is null")
        }
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

            // Pobierz dane z istniejącego dokumentu
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val userId = auth.currentUser?.uid
            val oldDayId = "${day.title}_$userId"
            val newDayId = "${newTitle}_$userId"
            val workoutPlansCollection = db.collection("users").document(userId!!).collection("workoutPlans")
            val oldDocRef = workoutPlansCollection.document(oldDayId)

            oldDocRef.get().addOnSuccessListener { document ->
                if (document != null) {
                    val data = document.data

                    // Update the document with the new title
                    oldDocRef.update("title", newTitle)

                    // Update the dayId in the workoutPlans documents
                    val workoutPlansCollection = db.collection("users").document(userId).collection("workoutPlans")
                    workoutPlansCollection.document(oldDayId).get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            val data = document.data
                            if (data != null) {
                                workoutPlansCollection.document(newDayId).set(data)
                                document.reference.delete()
                            }
                        }
                    }


                    // Update the dayId in the workoutResults documents
                    val workoutResultsCollection = db.collection("users").document(userId).collection("workoutResults")
                    workoutResultsCollection.whereEqualTo("dayId", oldDayId).get().addOnSuccessListener { documents ->
                        for (doc in documents) {
                            doc.reference.update("dayId", newDayId)
                        }
                    }

                    // Update the day title locally and notify the adapter
                    day.title = newTitle
                    daysAdapter.notifyDataSetChanged()
                }
            }
        }
        builder.setNegativeButton("Anuluj") { dialog, _ -> dialog.cancel() }

        builder.show()
    }


    override fun onDayClick(day: Day) {
        Log.d("EditPlanWorkout", "onDayClick: Day clicked: ${day.title}")
        if (day.title == null) {
            Log.e("EditPlanWorkout", "Day title is null")
        }
        val intent = Intent(this, SelectExerciseActivity::class.java)
        Log.d("EditPlanWorkout", "Sending day title: ${day.title}")
        intent.putExtra("Dzień", day.title)
        intent.putExtra("exerciseIds", day.exerciseIds.toTypedArray())
        startActivityForResult(intent, CreateWorkoutPlanActivity.REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            "EditPlanWorkout",
            "onActivityResult called with requestCode: $requestCode, resultCode: $resultCode"
        )
        Log.d("EditPlanWorkout", "onActivityResult: Received result from SelectExerciseActivity")

        if (requestCode == CreateWorkoutPlanActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedExercise = data?.getSerializableExtra("selectedExercise") as Exercise?
            val removedExercise = data?.getSerializableExtra("removedExercise") as Exercise?
            val dayTitle = data?.getStringExtra("Dzień")
            Log.d(
                "EditPlanWorkout",
                "onActivityResult: selectedExercise: ${selectedExercise?.name}, removedExercise: ${removedExercise?.name}, dayTitle: $dayTitle"
            )

            if (dayTitle != null) {
                val daysList = daysAdapter.getDays()
                for (day in daysList) {
                    if (day.title == dayTitle) {
                        if (selectedExercise != null) {
                            Log.d(
                                "EditPlanWorkout",
                                "Before add: Total exercises in the day: ${day.exercises.size}"
                            )
                            Log.d(
                                "EditPlanWorkout",
                                "Before add: Total exercises in the day: ${day.exercises.size}"
                            )
                            day.exercises.add(selectedExercise)
                            day.exerciseIds.add(selectedExercise.id)
                            Log.d(
                                "EditPlanWorkout",
                                "After add: Total exercises in the day: ${day.exercises.size}"
                            )
                            Log.d(
                                "EditPlanWorkout",
                                "onActivityResult: Added exercise: ${selectedExercise.name} to day: ${day.title}. Total exercises in the day: ${day.exercises.size}"
                            )
                        }
                        if (removedExercise != null) {
                            Log.d(
                                "EditPlanWorkout",
                                "Before remove: Total exercises in the day: ${day.exercises.size}"
                            )
                            val removeIndex =
                                day.exercises.indexOfFirst { it.id == removedExercise.id }
                            Log.d("EditPlanWorkout", "Index of removed exercise: $removeIndex")
                            if (removeIndex != -1) {
                                Log.d(
                                    "EditPlanWorkout",
                                    "Before remove: Total exercises in the day: ${day.exercises.size}"
                                )
                                day.exercises.removeAt(removeIndex)
                                day.exerciseIds.removeAt(removeIndex)
                                Log.d(
                                    "EditPlanWorkout",
                                    "After remove: Total exercises in the day: ${day.exercises.size}"
                                )
                                Log.d(
                                    "EditPlanWorkout",
                                    "onActivityResult: Removed exercise: ${removedExercise.name} from day: ${day.title}. Total exercises in the day: ${day.exercises.size}"
                                )
                            }
                        }

                        daysAdapter.notifyDataSetChanged()
                        break
                    }
                }
            } else {
                Log.e("EditPlanWorkout", "dayTitle is null")
            }
        } else {
            Log.e(
                "EditPlanWorkout",
                "Received unexpected requestCode: $requestCode or resultCode: $resultCode"
            )
        }
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Zapisz zmiany")
        builder.setMessage("Czy chcesz zapisać swoje zmiany?")

        builder.setPositiveButton("Tak") { dialog, id ->
            saveWorkoutPlan()
            super.onBackPressed()
        }

        builder.setNegativeButton("Nie") { dialog, id ->
            super.onBackPressed()
        }

        builder.setNeutralButton("Anuluj") { dialog, id ->
            // User cancelled the dialog, do nothing
        }

        builder.show()
    }

    fun saveWorkoutPlan() {
        val daysList = daysAdapter.getDays()
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // Dodane: sprawdzenie, czy wszystkie wartości sets i repetitions są nieujemne
        for (day in daysList) {
            for (exercise in day.exercises) {
                if (exercise.sets < 0 || exercise.repetitions < 0) {
                    Toast.makeText(
                        this,
                        "Nie można zapisać planu treningowego: liczba serii i powtórzeń musi być nieujemna",
                        Toast.LENGTH_LONG
                    ).show()
                    return  // Przerwij metodę i nie kontynuuj zapisywania
                }
            }
        }

        val userId = auth.currentUser?.uid
        if (userId != null) {
            // get reference to the collection
            val workoutPlansCollection = db.collection("users").document(userId).collection("workoutPlans")

            // Delete all documents from the collection
            workoutPlansCollection.get().addOnSuccessListener { querySnapshot ->
                for (doc in querySnapshot.documents) {
                    doc.reference.delete()
                }

                // Now add the documents from the list
                for ((index, day) in daysList.withIndex()) {
                    val exercisesList = day.exercises
                    val dayData = mutableMapOf<String, Any>()
                    dayData["title"] = day.title
                    dayData["exercises"] = exercisesList.map { exercise ->
                        mapOf(
                            "id" to exercise.id,
                            "name" to exercise.name,
                            "txtFile" to exercise.descriptionFile,
                            "mp4File" to exercise.videoFile,
                            "sets" to exercise.sets,
                            "repetitions" to exercise.repetitions,
                            "isAddedToDay" to exercise.isAddedToDay
                        )
                    }


                    // Use a combination of day index and userId as the dayId
                    val fixedDayId = "${index+1}_$userId"


                    workoutPlansCollection.document(fixedDayId).set(dayData)
                        .addOnSuccessListener {
                            Log.d("EditPlanWorkout", "Data successfully saved to Firebase")
                        }
                        .addOnFailureListener { e ->
                            Log.w("EditPlanWorkout", "Error adding workout day", e)
                        }
                }
            }
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        } else {
            Log.e("EditPlanWorkout", "User ID is null")
        }

    }

}