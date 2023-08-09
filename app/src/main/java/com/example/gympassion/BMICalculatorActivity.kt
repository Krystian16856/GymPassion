package com.example.gympassion

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.protobuf.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.ExecutionException

class BMICalculatorActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var height: Long? = null
    private var age: Int? = null
    private var weight: Double? = null
    private var sex: String? = null
    private var activityLevel: String? = null
    private var trainingLevel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bmicalculator)

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        title = "Kalkulator BMI"

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        sex = document.getString("sex")
                        if (sex != null) {
                            showWeightDialog()
                        } else {
                            // Here you can handle what to do if the user does not have sex data yet.
                        }
                    }
                }
        }

        checkAndFetchAgeHeight()
    }

    private fun checkAndFetchAgeHeight() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val docRef = db.collection("users").document(userId)
            docRef.get()
                .addOnSuccessListener { document ->
                    val user = document.data
                    height = user?.get("height") as? Long
                    val birthDate = user?.get("birthDate") as? com.google.firebase.Timestamp
                    if (birthDate != null) {
                        val birthDateLocalDate = birthDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        age = calculateAge(birthDateLocalDate)
                    }
                    if (height == null || age == null) {
                        showAgeHeightDialog()
                    } else {
                        checkAndFetchRemainingData()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Handle the case when there's no logged in user
        }
    }

    private fun checkAndFetchRemainingData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val docRef = db.collection("users").document(userId)
            docRef.get()
                .addOnSuccessListener { document ->
                    val user = document.data
                    weight = user?.get("weight") as? Double
                    sex = user?.get("sex") as? String
                    activityLevel = user?.get("activityLevel") as? String
                    trainingLevel = user?.get("trainingLevel") as? String
                    if (weight == null || sex == null || activityLevel == null || trainingLevel == null) {
                        showSexDialog()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Handle the case when there's no logged in user
        }
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
                    if (height in 50..250) {
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
                                        "birthDate" to timestamp
                                    )
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Dane zostały zaktualizowane.", Toast.LENGTH_SHORT)
                                        .show()
                                    // Kontynuuj proces
                                    checkAndFetchRemainingData()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(
                                        this,
                                        "Nie udało się zaktualizować danych: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
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



    private fun updateAgeAndHeightToFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("users").document(userId)
            userRef.update(
                "age", age,
                "height", height
            )
                .addOnSuccessListener {
                    Toast.makeText(this, "Dane zaktualizowane pomyślnie", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Błąd podczas aktualizacji danych: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            // Handle the case when there's no logged in user
        }
    }


    private fun showWeightDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Wprowadź swoją wagę")

        val weightInput = EditText(this)
        weightInput.hint = "Waga (kg)"
        dialogBuilder.setView(weightInput)
        weightInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL


        dialogBuilder.setPositiveButton("OK") { _, _ ->
            weight = weightInput.text.toString().toDoubleOrNull()
            if (weight != null) {
                showActivityLevelDialog()
            } else {
                Toast.makeText(this, "Please enter a valid weight", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
            goToDashboardActivity()
        }

        dialogBuilder.show()
    }

    private fun showSexDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Wybierz swoją płeć")

        val sexOptions = arrayOf("Mężczyzna", "Kobieta")
        val sexSpinner = Spinner(this)
        val sexAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sexOptions)
        sexSpinner.adapter = sexAdapter
        dialogBuilder.setView(sexSpinner)

        dialogBuilder.setPositiveButton("OK") { _, _ ->
            sex = sexSpinner.selectedItem.toString()
            showWeightDialog()
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
            goToDashboardActivity()
        }

        dialogBuilder.show()
    }

    private fun showActivityLevelDialog(selectedItem: Int = 0) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Wybierz swój poziom aktywności")

        val activityLevelOptions = arrayOf("Niski", "Średni", "Wysoki", "Bardzo aktywny", "Wyczynowy")
        val activityLevelSpinner = Spinner(this)
        val activityLevelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activityLevelOptions)
        activityLevelSpinner.adapter = activityLevelAdapter
        activityLevelSpinner.setSelection(selectedItem)
        dialogBuilder.setView(activityLevelSpinner)

        dialogBuilder.setPositiveButton("OK") { _, _ ->
            activityLevel = activityLevelSpinner.selectedItem.toString()
            showTrainingLevelDialog()
        }

        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
            goToDashboardActivity()
        }

        dialogBuilder.setNeutralButton("Info") { _, _ ->
            val info = "Niski: dla osoby chorej leżącej w łóżku\n\n" +
                    "Średni: dla umiarkowanej aktywności fizycznej\n\n" +
                    "Wysoki: aktywny tryb życia\n\n" +
                    "Bardzo aktywny: bardzo aktywny tryb życia\n\n" +
                    "Wyczynowy: wyczynowe uprawianie sportu"
            showInfoDialog("Poziom aktywności", info) { showActivityLevelDialog(activityLevelSpinner.selectedItemPosition) }
        }

        dialogBuilder.show()
    }

    private fun showInfoDialog(title: String, message: String, onDismiss: () -> Unit) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(title)
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            onDismiss()
        }
        dialogBuilder.show()
    }



    private fun showTrainingLevelDialog(selectedItem: Int = 0) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Wybierz swój poziom treningu")

        val trainingLevelOptions = arrayOf("Początkujący", "Średnio zaawansowany", "Zaawansowany")
        val trainingLevelSpinner = Spinner(this)
        val trainingLevelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, trainingLevelOptions)
        trainingLevelSpinner.adapter = trainingLevelAdapter
        trainingLevelSpinner.setSelection(selectedItem)
        dialogBuilder.setView(trainingLevelSpinner)

        dialogBuilder.setPositiveButton("OK") { _, _ ->
            trainingLevel = trainingLevelSpinner.selectedItem.toString()
            saveDataToFirebase()

            val intent = Intent(this, BMIResultsActivity::class.java)
            startActivity(intent)
        }


        dialogBuilder.setNegativeButton("Anuluj") { dialog, _ ->
            dialog.dismiss()
            goToDashboardActivity()
        }

        dialogBuilder.setNeutralButton("Info") { _, _ ->
            val info = "Początkujący: etap początkujący trwa zazwyczaj do 12 miesięcy od rozpoczęcia regularnych treningów.\n\n" +
                    "Średnio zaawansowany: etap ten trwa zazwyczaj od 12 do 24 miesięcy. Pojawiając się systematycznie na siłowni przez rok, jesteśmy już w stanie wyćwiczyć w sobie prawidłowe nawyki i poczuć większą siłę.\n\n" +
                    "Zaawansowany: poziom zaawansowany osiągają osoby, które ćwiczą na siłowni co najmniej 2 lata i nie zamierzają na tym poprzestać."
            showInfoDialog("Poziom zaawansowania treningu", info) { showTrainingLevelDialog(trainingLevelSpinner.selectedItemPosition) }
        }

        dialogBuilder.show()
    }



    private fun saveDataToFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null && weight != null && sex != null && activityLevel != null && trainingLevel != null) {
            val userRef = db.collection("users").document(userId)
            userRef.update(
                "weight", weight,
                "sex", sex,
                "activityLevel", activityLevel,
                "trainingLevel", trainingLevel
            )
                .addOnSuccessListener {
                    Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Failed to save data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            // Handle the case when there's no logged in user
        }
    }

    private fun calculateAge(birthDate: LocalDate?): Int {
        return if (birthDate != null) {
            val now = LocalDate.now()
            now.year - birthDate.year
        } else {
            0
        }
    }

    private fun goToDashboardActivity() {
        // Zainicjuj intencję do przejścia do DashboardActivity
        val intent = Intent(this, DashboardActivity::class.java)
        // Uruchom DashboardActivity
        startActivity(intent)
        // Zakończ obecną aktywność
        finish()
    }

}
