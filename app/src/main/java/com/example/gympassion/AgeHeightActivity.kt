package com.example.gympassion

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class AgeHeightActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var height: Long? = null
    private var age: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_age_height)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        showAgeHeightDialog()
    }

    private fun showAgeHeightDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Podaj datę urodzenia i wzrost")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val heightInput = EditText(this)
        heightInput.hint = "Wzrost (cm)"
        heightInput.inputType = InputType.TYPE_CLASS_NUMBER
        heightInput.setTag("height_input")
        layout.addView(heightInput)

        builder.setView(layout)

        builder.setPositiveButton("OK") { _, _ ->
            val datePicker = DatePickerDialog(this)
            datePicker.setOnDateSetListener { _, year, month, dayOfMonth ->
                val birthDate = LocalDate.of(year, month + 1, dayOfMonth)
                val age = calculateAge(birthDate)
                if (age < 6) {
                    Toast.makeText(this, "Wiek nie może być niższy niż 6 lat.", Toast.LENGTH_SHORT).show()
                    // Przejdź do DashboardActivity
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Zapisz do Firestore
                    val height = layout.findViewWithTag<EditText>("height_input").text.toString().toIntOrNull()
                    if (height != null && height in 50..250) {
                        showWorkoutMethodDialog(birthDate, height)
                    } else {
                        Toast.makeText(this, "Wprowadź poprawny wzrost (50-250 cm).", Toast.LENGTH_SHORT).show()
                        // Przejdź do DashboardActivity
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
            datePicker.show()
        }

        builder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
            // Przejdź do DashboardActivity
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
        builder.show()
    }


    private fun showWorkoutMethodDialog(birthDate: LocalDate, height: Int) {
        val workoutMethods = arrayOf("FBW", "Push Pull", "Push Pull Legs", "Split", "Trening obwodowy")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wybierz metodę treningu")

        val workoutMethodSpinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, workoutMethods)
        workoutMethodSpinner.adapter = adapter
        builder.setView(workoutMethodSpinner)

        builder.setPositiveButton("OK") { _, _ ->
            val workoutMethod = workoutMethodSpinner.selectedItem.toString()
            saveDataToFirestore(birthDate, height, workoutMethod)
        }

        builder.setNegativeButton("Anuluj") { _, _ ->
            // Przejdź do DashboardActivity
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        builder.show()
    }

    private fun saveDataToFirestore(birthDate: LocalDate, height: Int, workoutMethod: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Convert LocalDate to Date
            val date = Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            // Convert Date to Timestamp
            val timestamp = com.google.firebase.Timestamp(date)
            db.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "height" to height,
                        "birthDate" to timestamp,
                        "workoutMethod" to workoutMethod
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Dane zostały zaktualizowane.", Toast.LENGTH_SHORT)
                        .show()
                    // Przejdź do ProfileActivity
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Nie udało się zaktualizować danych: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun calculateAge(birthDate: LocalDate): Int {
        val now = LocalDate.now()
        return now.year - birthDate.year
    }
}
