package com.example.gympassion

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.DropBoxManager
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginBottom
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import me.grantland.widget.AutofitTextView
import soup.neumorphism.NeumorphButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WorkoutHistoryActivity : AppCompatActivity() {
    private lateinit var daysContainer: LinearLayout
    private lateinit var chartsContainer: LinearLayout
    private var axisTextColor: Int = Color.BLACK
    private var titleTextColor: Int = Color.BLACK
    private var legendTextColor: Int = Color.BLACK
    private lateinit var spinner: Spinner
    private lateinit var tableLayout: TableLayout


    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_history)

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }


        tableLayout = findViewById(R.id.exercise_table)

        if (isNightMode()) {
            axisTextColor = Color.WHITE
            titleTextColor = Color.WHITE
            legendTextColor = Color.WHITE
        }



        title = "Statystyka"

        daysContainer = findViewById(R.id.days_container)
        chartsContainer = findViewById(R.id.charts_container)

        userId = FirebaseAuth.getInstance().currentUser?.uid
        loadWorkoutPlans()
    }


    private fun loadWorkoutPlans() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            val workoutPlanCollection = db.collection("users").document(userId).collection("workoutPlans")
            workoutPlanCollection.get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val title = document.getString("title")  // get the title

                        // Tworzenie przycisku dla każdego dnia
                        val button = NeumorphButton(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                500,  // Fixed width
                                250   // Fixed height
                            )
                            text = title ?: document.id  // use the title as it is, no replacements
                            setSingleLine()  // make sure the text is in a single line
                            gravity = Gravity.CENTER  // center the text
                            setMaxLines(1)  // make sure the text is in a single line
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)  // set a base text size
                            setTypeface(null, Typeface.BOLD)  // make the text bold
                            setOnClickListener {
                                // Wywołanie metody, która będzie pobierać wyniki treningów dla danego dnia
                                showWorkoutResult(document.id)  // still using document id here for unique identification
                            }
                            val isDarkTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                            if (isDarkTheme) {
                                setShadowColorDark(ContextCompat.getColor(this@WorkoutHistoryActivity, R.color.shadow_dark))
                                setShadowColorLight(ContextCompat.getColor(this@WorkoutHistoryActivity, R.color.shadow_light))
                            } else {
                                setShadowColorDark(ContextCompat.getColor(this@WorkoutHistoryActivity, R.color.shadow_dark))
                                setShadowColorLight(ContextCompat.getColor(this@WorkoutHistoryActivity, R.color.shadow_light))
                            }
                        }

                        daysContainer.addView(button)
                    }
                }
        } else {
            Log.e("WorkoutHistoryActivity", "User ID is null")
        }
    }


    private fun showWorkoutResult(dayId: String) {
        // Usuń wszystkie wykresy przed dodaniem nowych
        chartsContainer.removeAllViews()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            getWorkoutResults(userId, dayId) { workoutResults ->
                // Sprawdź, czy lista workoutResults jest pusta lub null
                if (workoutResults == null || workoutResults.isEmpty()) {
                    // Wyświetl komunikat, gdy brak danych
                    Toast.makeText(this, "Brak danych. Proszę przeprowadzić trening.", Toast.LENGTH_LONG).show()
                    return@getWorkoutResults
                }
                val exercises = workoutResults.groupBy { it.exerciseName }

                exercises.forEach { (exerciseName, results) ->
                    val dataContainer = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            // Dodaj dolny margines
                            bottomMargin = 10.dpToPx()
                        }
                    }

                    val title = TextView(this).apply {
                        text = exerciseName
                        textSize = 34f
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER_HORIZONTAL

                        // Ustalanie tła z atrybutu stylu
                        val attrs = intArrayOf(R.attr.exerciseBackground)
                        val typedArrayBackground = context.obtainStyledAttributes(attrs)
                        val backgroundResource = typedArrayBackground.getResourceId(0, 0)
                        setBackgroundResource(backgroundResource)
                        typedArrayBackground.recycle()

                        val textColor: ColorStateList? = if (isNightMode()) {
                            ResourcesCompat.getColorStateList(
                                resources,
                                R.color.exercise_text_color_dark,
                                theme
                            )
                        } else {
                            ResourcesCompat.getColorStateList(
                                resources,
                                R.color.exercise_text_color_light,
                                theme
                            )
                        }
                        setTextColor(textColor?.defaultColor ?: Color.BLACK)
                    }

                    dataContainer.addView(title)

                    val chart = createChartForExercise(exerciseName, results)
                    dataContainer.addView(chart)

                    val spinner = Spinner(this@WorkoutHistoryActivity)
                    dataContainer.addView(spinner)
                    updateSpinner(this@WorkoutHistoryActivity, exerciseName, results, spinner)

                    val tableLayout = TableLayout(this@WorkoutHistoryActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    dataContainer.addView(tableLayout)

                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val selectedDate = parent.getItemAtPosition(position).toString()
                            updateExerciseTable(selectedDate, exerciseName, results, tableLayout)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // Implement your logic here if needed
                        }
                    }

                    chartsContainer.addView(dataContainer)
                }
            }
        }
    }


    private fun updateExerciseTable(selectedDate: String, exerciseName: String, workoutResults: List<WorkoutResult>, tableLayout: TableLayout) {
        // Clear the table first
        tableLayout.removeAllViews()

        // Inflate and add the header row
        val inflater = LayoutInflater.from(this)
        val header = inflater.inflate(R.layout.exercise_header_row2, tableLayout, false) as TableRow
        val headerRepetitionsAndWeights = header.findViewById<TextView>(R.id.metricTextView)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).collection("settings").document("userSettings").get()
                .addOnSuccessListener { document ->
                    val isMetric = document.getBoolean("metric") ?: false
                    val metricTextValue = if (isMetric) "Kg" else "Lb"

                    headerRepetitionsAndWeights.text = "Powtórzenia x $metricTextValue"
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Błąd: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
        tableLayout.addView(header)

        // Dodanie linii pod nagłówkiem
        val headerLine = View(this).apply {
            layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 3.dpToPx())
            setBackgroundColor(Color.parseColor("#FFA500")) // Kolor pomarańczowy
        }
        tableLayout.addView(headerLine)

        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // Filter the results based on selected date and exercise name
        val filteredResults =
            workoutResults.filter { sdf.format(Date(it.timestamp * 1000)) == selectedDate && it.exerciseName == exerciseName }

        filteredResults.forEach { result ->
            // Assuming the lengths of series, repetitions and weights are the same
            for (i in 0 until result.series) {
                val row = inflater.inflate(R.layout.exercise_row2, tableLayout, false) as TableRow
                val seriesText = row.findViewById<TextView>(R.id.series)
                seriesText.text = (i + 1).toString()  // The series number starts from 1 and increases by 1

                val repetitionsAndWeightsText = row.findViewById<TextView>(R.id.repetitionsAndWeights)
                repetitionsAndWeightsText.text = "${result.previousWeekWeight[i]}"

                val durationText = row.findViewById<TextView>(R.id.duration)
                if (i == result.series / 2) {  // Show the duration at the middle row
                    durationText.text = "${result.duration}"
                    durationText.setTypeface(null, Typeface.BOLD)
                }

                // Add row to table without a divider line
                tableLayout.addView(row)
            }
            val finalLine = View(this).apply {
                layoutParams =
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 3.dpToPx())
                setBackgroundColor(Color.parseColor("#FFA500")) // Kolor pomarańczowy
            }
            tableLayout.addView(finalLine)

            tableLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 30.dpToPx()
            }
        }
    }



    private fun updateSpinner(context: Context, exerciseName: String, workoutResults: List<WorkoutResult>, spinner: Spinner) {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val dates = workoutResults
            .filter { it.exerciseName == exerciseName }
            .sortedBy { it.timestamp }
            .map { sdf.format(Date(it.timestamp * 1000)) }  // Convert timestamp to formatted date string
            .distinct()


        val spinnerArrayAdapter = object : ArrayAdapter<String>(
            context,
            R.layout.custom_spinner_item,
            dates
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setBackgroundColor(Color.parseColor("#FFA500")) // Kolor pomarańczowy
                val textView = view as TextView
                textView.setTextColor(Color.WHITE) // Kolor tekstu
                textView.gravity = Gravity.CENTER // Wyśrodkowany tekst
                return view
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                view.setBackgroundColor(Color.parseColor("#FFA500")) // Kolor pomarańczowy
                val textView = view as TextView
                textView.setTextColor(Color.WHITE) // Kolor tekstu
                textView.gravity = Gravity.CENTER // Wyśrodkowany tekst
                return view
            }
        }
        spinner.adapter = spinnerArrayAdapter
        spinner.setBackgroundResource(R.color.orange) // Ustawienie tła przycisku spinnera na pomarańczowe
    }

    private fun getWorkoutResults(
        userId: String,
        dayId: String,
        callback: (List<WorkoutResult>) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val workoutResultCollection = db.collection("users")
            .document(userId)
            .collection("workoutResults")
            .document(dayId)
            .collection("exercises")
        workoutResultCollection.get()
            .addOnSuccessListener { documents ->
                val workoutResults = documents.mapNotNull { document ->
                    val timestamp =
                        (document.get("timestamp") as? com.google.firebase.Timestamp)?.seconds
                    val series = document.getLong("series")?.toInt()
                    val repetitions = document.get("repetitions") as? List<Long>
                    val weights = document.get("weights") as? List<Long>
                    val weightsAdjusted = weights?.map { if (it == 0L) 1L else it }
                    val previousWeekWeight = document.get("previousWeekWeight") as? List<String> ?: emptyList()
                    val exerciseName = document.getString("exerciseName") ?: ""
                    val durationSeconds = document.getLong("duration") ?: 0L
                    val hours = TimeUnit.SECONDS.toHours(durationSeconds)
                    val minutes = TimeUnit.SECONDS.toMinutes(durationSeconds) - TimeUnit.HOURS.toMinutes(hours)
                    val seconds = durationSeconds - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes)
                    val formattedDuration = String.format("%02d:%02d:%02d", hours, minutes, seconds)


                    if (timestamp != null && series != null && repetitions != null && weightsAdjusted != null && exerciseName.isNotEmpty()) {
                        val load = series * repetitions.zip(weightsAdjusted).map { it.first * it.second }.sum()
                        WorkoutResult(date = timestamp, load = load.toFloat(), exerciseName = exerciseName, previousWeekWeight = previousWeekWeight, series = series, timestamp = timestamp, duration = formattedDuration)
                    } else {
                        null
                    }



                }
                callback(workoutResults)
            }
            .addOnFailureListener { exception ->
                Log.w("WorkoutHistoryActivity", "Error loading workout results", exception)
            }
    }

    private fun createChartForExercise(exerciseName: String, workoutResults: List<WorkoutResult>): LineChart {
        val chart = LineChart(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2000 // Wysokość wykresu
            ).apply {
                bottomMargin = 150 // Dolny margines dla wykresu
            }
        }

        val sortedWorkoutResults = workoutResults.sortedBy { it.date }
        val entries = sortedWorkoutResults.mapIndexed { index, result ->
            com.github.mikephil.charting.data.Entry(index.toFloat(), result.load)
        }
        val dataSet = LineDataSet(entries, "Obciążenie treningowe") // Nazwa ćwiczenia w tytule



        // konfiguracja dataSet
        dataSet.color = Color.RED
        dataSet.lineWidth = 6f // Grubość linii

        dataSet.setCircleColor(Color.BLUE) // Kolor kółek
        dataSet.setCircleRadius(14f) // Rozmiar kółek
        dataSet.circleHoleRadius = 7f // Promień otworu w kółku
        dataSet.setDrawCircleHole(true) // Rysowanie otworu w kółku

        dataSet.valueTextSize = 15f // Rozmiar tekstu wartości
        dataSet.valueTextColor = Color.RED // Kolor tekstu wartości
        dataSet.valueTypeface = Typeface.DEFAULT_BOLD // Pogrubienie tekstu wartości

        val lineData = LineData(dataSet)
        chart.data = lineData


        // Konfiguracja osi X
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 12f // Rozmiar tekstu
        xAxis.textColor = axisTextColor // Kolor tekstu
        xAxis.typeface = Typeface.DEFAULT_BOLD // Pogrubienie tekstu
        xAxis.setDrawGridLines(false) // Wyłączenie siatki
        xAxis.setDrawAxisLine(true)


        // Konfiguracja osi Y
        val leftAxis = chart.axisLeft
        leftAxis.textSize = 16f // Rozmiar tekstu
        leftAxis.textColor = axisTextColor // Kolor tekstu
        leftAxis.typeface = Typeface.DEFAULT_BOLD // Pogrubienie tekstu
        leftAxis.setDrawGridLines(false) // Wyłączenie siatki
        xAxis.setDrawAxisLine(true)

        // konfiguracja dataSet
        dataSet.color = Color.RED
        dataSet.lineWidth = 6f // Grubość linii
        dataSet.setDrawFilled(true) // Włączenie podkreślenia pod linią wykresu
        dataSet.fillColor = Color.DKGRAY// Ustawienie koloru podkreślenia


        if (isNightMode()) {
            xAxis.textColor = Color.YELLOW
            xAxis.setAxisLineColor(Color.WHITE) // Kolor linii osi
            leftAxis.textColor = Color.YELLOW
            leftAxis.setAxisLineColor(Color.WHITE) // Kolor linii osi
            xAxis.setGridColor(Color.WHITE) // Ustaw kolor siatki
            leftAxis.setGridColor(Color.WHITE) // Ustaw kolor siatki
            dataSet.fillColor = Color.LTGRAY
        }



        chart.axisRight.isEnabled = false // Wyłączenie prawej osi x
        chart.animateXY(1500, 1000)

        // Ustawianie formatera dla osi x
        val dates = sortedWorkoutResults.mapIndexed { index, result ->
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = sdf.format(Date(result.date * 1000))
            index.toFloat() to date
        }.toMap()
        chart.xAxis.valueFormatter = DateValueFormatter(dates)

        // Umożliwienie przewijania i skalowania wykresu
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        // Ustawianie maksymalnej liczby widocznych wpisów na osi x
        chart.setVisibleXRangeMaximum(3f)

        xAxis.labelRotationAngle = -45f

        // Ustawienie wielkości tekstu legendy
        chart.legend.textSize = 14f
        chart.legend.textColor = legendTextColor

        chart.invalidate() // odświeżanie wykresu

        return chart
    }





    class DateValueFormatter(private val dates: Map<Float, String>) : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            // Pobierz datę odpowiadającą wartości na osi X, jeśli nie ma takiej daty, zwróć pusty string.
            return dates[value] ?: ""
        }
    }

    fun isNightMode(): Boolean {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }


    // Extension function to convert dp to pixels
    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
}

