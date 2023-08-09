package com.example.gympassion

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import soup.neumorphism.NeumorphButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import soup.neumorphism.ShapeType

class StartWorkoutActivity : AppCompatActivity() {
    private lateinit var daysContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_workout)

        title = "Wybór treningu"

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }


        daysContainer = findViewById(R.id.days_container)
        loadWorkoutPlan()
    }

    private fun loadWorkoutPlan() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            val workoutPlanCollection = db.collection("users").document(userId).collection("workoutPlans")
            workoutPlanCollection.get()
                .addOnSuccessListener { documents ->
                    for ((index, document) in documents.withIndex()) {
                        val title = document.getString("title") ?: "Default Title"
                        val fixedDayId = "${index + 1}_$userId"

                        val button = NeumorphButton(this).apply {
                            text = title
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                gravity = Gravity.CENTER_HORIZONTAL
                                topMargin = 16.dpToPx()
                                bottomMargin = 16.dpToPx()
                            }

                            // Use fixedDayId to redirect to DayWorkoutActivity
                            setOnClickListener {
                                val intent = Intent(
                                    this@StartWorkoutActivity,
                                    DayWorkoutActivity::class.java
                                )
                                intent.putExtra("dayId", fixedDayId)
                                intent.putExtra("dayTitle", title)
                                startActivity(intent)
                            }
                            val isDarkTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                            if (isDarkTheme) {
                                setShadowColorDark(ContextCompat.getColor(this@StartWorkoutActivity, R.color.shadow_dark))
                                setShadowColorLight(ContextCompat.getColor(this@StartWorkoutActivity, R.color.shadow_light))
                            } else {
                                setShadowColorDark(ContextCompat.getColor(this@StartWorkoutActivity, R.color.shadow_dark))
                                setShadowColorLight(ContextCompat.getColor(this@StartWorkoutActivity, R.color.shadow_light))
                            }
                        }
                        daysContainer.addView(button)

                        val separator = View(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                2.dpToPx()
                            ).apply {
                                topMargin = 8.dpToPx()
                                bottomMargin = 8.dpToPx()
                            }
                            setBackgroundColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.separator
                                )
                            )
                        }
                        daysContainer.addView(separator)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("StartWorkoutActivity", "Error loading workout plan", exception)
                }
        } else {
            Log.e("StartWorkoutActivity", "User ID is null")
        }
    }



    // Extension function to convert dp to pixels
        fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    }