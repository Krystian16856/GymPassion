package com.example.gympassion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class BMIResultsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bmiresults)

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        val progressBar = findViewById<ProgressBar>(R.id.bmiProgressBar)
        val indicator = findViewById<View>(R.id.bmiIndicator)

        val updateDataButton = findViewById<soup.neumorphism.NeumorphButton>(R.id.updateDataButton)
        updateDataButton.setOnClickListener {
            val intent = Intent(this, BMICalculatorActivity::class.java).apply {
                putExtra("updateData", true)
            }
            startActivity(intent)
        }





        title = "BMI"

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val height = document.getDouble("height")
                        val weight = document.getDouble("weight")
                        val sex = document.getString("sex")
                        val activityLevel = document.getString("activityLevel")
                        val trainingLevel = document.getString("trainingLevel")

                        Log.d("BMIResultsActivity", "height: $height")
                        Log.d("BMIResultsActivity", "weight: $weight")
                        Log.d("BMIResultsActivity", "sex: $sex")
                        Log.d("BMIResultsActivity", "activityLevel: $activityLevel")
                        Log.d("BMIResultsActivity", "trainingLevel: $trainingLevel")

                        // Convert Timestamp to LocalDate
                        val birthDate = (document.get("birthDate") as com.google.firebase.Timestamp).toDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate()

                        Log.d("BMIResultsActivity", "birthDate: $birthDate")

                        if (height != null && weight != null) {
                            val heightInMeters = height / 100.0
                            val bmi = weight / (heightInMeters * heightInMeters)

                            val age = calculateAge(birthDate)



                            Log.d("BMIResultsActivity", "age: $age")

                            val bmr: Double
                            if (sex == "Mężczyzna") {
                                bmr = 66.5 + (13.75 * weight) + (5.003 * height) - (6.755 * age)
                            } else {
                                bmr = 655.1 + (9.563 * weight) + (1.85 * height) - (4.676 * age)
                            }

                            Log.d("BMIResultsActivity", "bmr: $bmr")

                            val activityMultiplier = when (activityLevel) {
                                "Niski" -> 1.2
                                "Średni" -> 1.4
                                "Wysoki" -> 1.6
                                "Bardzo aktywny" -> 1.75
                                "Wyczynowy" -> 2.0
                                else -> 1.2
                            }

                            Log.d("BMIResultsActivity", "activityMultiplier: $activityMultiplier")

                            val tdee = bmr * activityMultiplier
                            val caloricNeedForMaintenance = tdee.roundToInt()
                            val caloricNeedForFatLoss = (tdee - (tdee * 0.2)).roundToInt()
                            val caloricNeedForMuscleGain = (tdee + (tdee * 0.2)).roundToInt()

                            Log.d("BMIResultsActivity", "caloricNeedForMaintenance: $caloricNeedForMaintenance")
                            Log.d("BMIResultsActivity", "caloricNeedForFatLoss: $caloricNeedForFatLoss")
                            Log.d("BMIResultsActivity", "caloricNeedForMuscleGain: $caloricNeedForMuscleGain")

                            // Assuming you have TextViews in your layout for displaying these values
                            val bmiTextView = findViewById<TextView>(R.id.bmiTextView)
                            val bmiRatingTextView = findViewById<TextView>(R.id.bmiRatingTextView)
                            val caloricNeedTextView = findViewById<TextView>(R.id.caloricNeedTextView)
                            val trainingLevelTextView = findViewById<TextView>(R.id.trainingLevelTextView)
                            val caloricNeedForFatLossTextView =
                                findViewById<TextView>(R.id.caloricNeedForFatLossTextView)
                            val caloricNeedForMuscleGainTextView =
                                findViewById<TextView>(R.id.caloricNeedForMuscleGainTextView)

                            bmiTextView.text = "BMI: ${bmi.roundToInt()}"
                            bmiRatingTextView.text = getBMIRating(bmi)
                            caloricNeedTextView.text = "$caloricNeedForMaintenance kcal"
                            trainingLevelTextView.text = "$trainingLevel"
                            caloricNeedForFatLossTextView.text =
                                "$caloricNeedForFatLoss kcal"
                            caloricNeedForMuscleGainTextView.text =
                                "$caloricNeedForMuscleGain kcal"


                            progressBar.post {
                                val params = indicator.layoutParams as RelativeLayout.LayoutParams
                                params.marginStart = (progressBar.width * (bmi / 40)).toInt() - indicator.width / 2
                                indicator.layoutParams = params
                            }
                        }
                    }
                }
        }
    }

    private fun calculateAge(birthDate: LocalDate): Int {
        val now = LocalDate.now()
        return now.year - birthDate.year
    }

    override fun onBackPressed() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun getBMIRating(bmi: Double): String {
        return when {
            bmi < 16 -> "Wygłodzenie"
            bmi < 17 -> "Wychudzenie"
            bmi < 18.5 -> "Niedowaga"
            bmi < 25 -> "Wartość prawidłowa"
            bmi < 30 -> "Nadwaga"
            bmi < 35 -> "Otyłość I stopnia"
            bmi < 40 -> "Otyłość II stopnia"
            else -> "Otyłość III stopnia"
        }
    }
}
