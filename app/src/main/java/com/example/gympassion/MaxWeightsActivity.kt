package com.example.gympassion

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Locale

class MaxWeightsActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var maxWeightsRecyclerView: RecyclerView
    private lateinit var maxWeightsAdapter: MaxWeightsAdapter
    private val maxWeightsData = mutableListOf<MaxWeight>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_max_weights)

        maxWeightsRecyclerView = findViewById(R.id.max_weights_recycler_view)
        maxWeightsRecyclerView.layoutManager = LinearLayoutManager(this)

        maxWeightsAdapter = MaxWeightsAdapter(maxWeightsData)
        maxWeightsRecyclerView.adapter = maxWeightsAdapter

        if (userId != null) {
            getAllDayIdsForUser(userId)
        } else {
            Log.e("MaxWeightsActivity", "User ID is null")
        }

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getAllDayIdsForUser(userId: String) {
        val dayIds = mutableListOf<String>()

        db.collection("users").document(userId).collection("workoutResults").get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result) {
                        dayIds.add(document.id)
                    }
                    // Gdy już mamy wszystkie ID dni, możemy załadować maksymalne wagi
                    loadMaxWeightsForUser(userId, dayIds)
                } else {
                    Log.d(TAG, "Error getting documents: ", task.exception)
                }
            }
    }


    private fun loadMaxWeightsForUser(userId: String, dayIds: List<String>) {
        // Get user settings
        db.collection("users").document(userId)
            .collection("settings").document("userSettings")
            .get()
            .addOnSuccessListener { document ->
                val metric = document.getBoolean("metric") ?: true
                Log.d("MaxWeightsActivity", "Got user settings: metric = $metric")

                val exercisesData = mutableMapOf<String, Pair<Double, String>>()
                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (dayId in dayIds) {
                    Log.d("MaxWeightsActivity", "Processing day: $dayId")

                    tasks.add(
                        db.collection("users").document(userId)
                            .collection("workoutResults").document(dayId)
                            .collection("exercises")
                            .get()
                    )
                }

                Tasks.whenAllSuccess<QuerySnapshot>(tasks).addOnSuccessListener { results ->
                    Log.d("MaxWeightsActivity", "Received workout results from all days")

                    results.forEach { exerciseResult ->
                        for (exerciseDocument in exerciseResult) {
                            val exerciseName = exerciseDocument.getString("exerciseName")
                            val weights = exerciseDocument.get("weights") as? List<Number>
                            val repetitions = exerciseDocument.get("repetitions") as? List<Number>
                            val timestamp = exerciseDocument.getTimestamp("timestamp")

                            Log.d("MaxWeightsActivity", "Processing exercise: name = $exerciseName, weights = $weights, repetitions = $repetitions, timestamp = $timestamp")

                            if (exerciseName != null && weights != null && repetitions != null && timestamp != null) {
                                for ((index, weight) in weights.withIndex()) {
                                    val repetition = repetitions.getOrNull(index)
                                    if (repetition != null) {
                                        val currentMax = exercisesData[exerciseName]?.first ?: 0.0
                                        val weightAsDouble = weight.toDouble()
                                        if (weightAsDouble > currentMax) {
                                            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                            val formattedDate = sdf.format(timestamp.toDate())
                                            exercisesData[exerciseName] = Pair(weightAsDouble, formattedDate)
                                            Log.d("MaxWeightsActivity", "New max weight for $exerciseName: $weightAsDouble on $formattedDate")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Update max weights data
                    maxWeightsData.clear()
                    maxWeightsData.addAll(exercisesData.map { (exercise, data) ->
                        val (maxWeight, date) = data
                        MaxWeight(exercise, maxWeight, date, metric)
                    })

                    Log.d("MaxWeightsActivity", "Updated max weights data: $maxWeightsData")

                    // Notify adapter about data changes
                    maxWeightsAdapter.notifyDataSetChanged()
                }.addOnFailureListener { exception ->
                    Log.d("MaxWeightsActivity", "Failed to get workout results", exception)
                }
            }
            .addOnFailureListener { exception ->
                Log.d("MaxWeightsActivity", "Failed to get user settings", exception)
            }
    }



}

