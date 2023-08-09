package com.example.gympassion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.gympassion.databinding.ActivityScheduleWorkoutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import soup.neumorphism.NeumorphButton
import soup.neumorphism.NeumorphCardView
import soup.neumorphism.ShapeType

class ScheduleWorkoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScheduleWorkoutBinding
    private val selectedHours = mutableMapOf<String, String>()


    private val selectedDays = mutableMapOf(
        "monday" to false,
        "tuesday" to false,
        "wednesday" to false,
        "thursday" to false,
        "friday" to false,
        "saturday" to false,
        "sunday" to false
    )

    private fun toggleDay(day: String, card: NeumorphCardView, spinner: Spinner) {
        selectedDays[day] = !selectedDays[day]!!

        if (selectedDays[day] == true) {
            card.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOnClick))
        } else {
            card.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOnRelease))
            // Ustaw spinner na "Godzina" gdy dzień jest odznaczony
            spinner.setSelection(0)
            selectedHours.remove(day)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        title = "Dni treningowe"



        val daysNames = listOf(
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday",
            "saturday",
            "sunday"
        )

        val daysSpinners = listOf(
            binding.spinnerStartMonday,
            binding.spinnerStartTuesday,
            binding.spinnerStartWednesday,
            binding.spinnerStartThursday,
            binding.spinnerStartFriday,
            binding.spinnerStartSaturday,
            binding.spinnerStartSunday,
        )

        val hours = resources.getStringArray(R.array.workout_hours).toList()
        val adapter = CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, hours)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        daysSpinners.forEach { spinner ->
            spinner.adapter = adapter
        }



        val daysSpinnerListeners = daysSpinners.mapIndexed { index, spinner ->
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    if (selectedDays[daysNames[index]] == true) {
                        selectedHours[daysNames[index]] = parent.getItemAtPosition(pos).toString()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    if (parent.selectedItem.toString() == "Godzina" && selectedDays[daysNames[index]] == true) {
                        selectedHours.remove(daysNames[index])
                    }
                }
            }
        }


        val daysCards = listOf(
            binding.mondayCard,
            binding.tuesdayCard,
            binding.wednesdayCard,
            binding.thursdayCard,
            binding.fridayCard,
            binding.saturdayCard,
            binding.sundayCard
        )

        val daysRows = listOf(
            binding.mondayRow,
            binding.tuesdayRow,
            binding.wednesdayRow,
            binding.thursdayRow,
            binding.fridayRow,
            binding.saturdayRow,
            binding.sundayRow
        )


        daysSpinners.forEach { spinner ->
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    val dayIndex = daysSpinners.indexOf(spinner)
                    selectedHours[daysNames[dayIndex]] = parent.getItemAtPosition(pos).toString()

                    if (selectedDays[daysNames[dayIndex]] == false && parent.getItemAtPosition(pos).toString() != "Godzina") {
                        toggleDay(daysNames[dayIndex], daysCards[dayIndex], spinner)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }
        }


        daysRows.forEachIndexed { index, row ->
            row.setOnClickListener {
                toggleDay(daysNames[index], daysCards[index], daysSpinners[index])
            }
        }


        binding.nextButton.setOnClickListener {
            val anyDaysSelected = selectedDays.values.any { it == true }
            val allHoursSelected =
                selectedDays.keys.none { selectedDays[it]!! && selectedHours[it] == "Godzina" }
            val hoursCountCorrect =
                selectedDays.values.count { it } == selectedHours.values.count { it != "Godzina" }

            if (!anyDaysSelected || !allHoursSelected || !hoursCountCorrect) {
                daysCards.forEach { card ->
                    val day = daysNames[daysCards.indexOf(card)]
                    if (selectedDays[day]!! && (!selectedHours.containsKey(day) || selectedHours[day] == "Godzina")) {
                        // Dla zaznaczonego dnia, który nie ma wybranej godziny, potrząśnij kartą
                        shakeView(card)
                    } else if (!anyDaysSelected || !allHoursSelected || !hoursCountCorrect) {
                        // Jeżeli nie wybrano żadnego dnia, lub dla któregoś z wybranych dni nie ma godziny, lub liczba godzin nie jest prawidłowa, potrząśnij wszystkimi kartami
                        shakeView(card)
                    }
                }
                Toast.makeText(
                    this,
                    "Wybierz przynajmniej jeden dzień i godzinę dla każdego zaznaczonego dnia",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                saveSelectedDays()
            }
        }
    }

    private fun saveSelectedDays() {
        Log.d("ScheduleWorkoutActivity", "Saving selected days")

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d("ScheduleWorkoutActivity", "User ID: $userId")

            val selectedDaysCollection = db.collection("users").document(userId).collection("selectedDays")
            selectedDaysCollection.add(selectedDays)
                .addOnSuccessListener { documentReference ->
                    Log.d("ScheduleWorkoutActivity", "Selected days saved with ID: ${documentReference.id}")

                    val selectedHoursCollection = db.collection("users").document(userId).collection("selectedHours")
                    selectedHoursCollection.add(selectedHours)
                        .addOnSuccessListener {
                            Log.d("ScheduleWorkoutActivity", "Selected hours saved")

                            val intentSettings = Intent(this, SettingsActivity::class.java)
                            startActivity(intentSettings)
                        }
                        .addOnFailureListener { e ->
                            Log.w("ScheduleWorkoutActivity", "Error adding selected hours", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.w("ScheduleWorkoutActivity", "Error adding selected days", e)
                }
        } else {
            Log.e("ScheduleWorkoutActivity", "User ID is null")
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
