package com.example.gympassion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class SelectExerciseActivity : AppCompatActivity(), OnExerciseClickListener, CategoriesAdapter.OnCategoryClickListener {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var categoryList: RecyclerView
    private lateinit var categoryAdapter: CategoriesAdapter
    private lateinit var categoryLayoutManager: RecyclerView.LayoutManager

    private lateinit var exerciseList: RecyclerView
    private lateinit var exerciseAdapter: ExercisesAdapter
    private lateinit var exerciseLayoutManager: RecyclerView.LayoutManager

    private lateinit var storage: FirebaseStorage
    private var dayTitle: String? = null

    private var allExercises = mutableListOf<Exercise>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_exercise)
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        val searchExerciseInput = findViewById<EditText>(R.id.search_exercise_input)
        searchExerciseInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // nie robimy nic przed zmianą tekstu
            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Aktualizujemy listę ćwiczeń po każdej zmianie tekstu
                val searchText = s.toString().toLowerCase(Locale.ROOT)
                val filteredExercises = allExercises.filter { exercise ->
                    exercise.name.toLowerCase(Locale.ROOT).contains(searchText)
                }
                exerciseAdapter.submitList(filteredExercises)
            }

            override fun afterTextChanged(s: Editable) {
                // nie robimy nic po zmianie tekstu
            }
        })



        categoryList = findViewById(R.id.category_list)
        categoryLayoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoriesAdapter(this, R.layout.category_list_item, this)
        categoryList.layoutManager = categoryLayoutManager
        categoryList.adapter = categoryAdapter

        exerciseList = findViewById(R.id.exercise_list)
        exerciseLayoutManager = LinearLayoutManager(this)
        exerciseAdapter = ExercisesAdapter(this, false, R.layout.exercise_list_item)
        exerciseList.layoutManager = exerciseLayoutManager
        exerciseList.adapter = exerciseAdapter

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        fetchCategories()

    }

    private val categoryNamesMap = mapOf(
        "Back" to "Plecy",
        "Biceps" to "Biceps",
        "Chest" to "Klatka piersiowa",
        "Forearm" to "Przedramię",
        "Leg" to "Nogi",
        "Shoulder" to "Barki",
        "Triceps" to "Triceps"
    )

    private val categoryNamesMapReverse =
        categoryNamesMap.entries.associateBy({ it.value }) { it.key }

    private fun fetchCategories() {
        val categories = listOf("Plecy", "Biceps", "Klatka piersiowa", "Przedramię", "Nogi", "Barki", "Triceps")
        categoryAdapter.submitList(categories)
    }


    private fun fetchExercisesForCategory(category: String) {
        val categoryRef = firestore.collection(category)
        categoryRef.get().addOnSuccessListener { documents ->
            val exercises = mutableListOf<Exercise>()
            for (document in documents) {
                val exercise = Exercise(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    descriptionFile = document.getString("description") ?: "",
                    videoFile = document.getString("videoFile") ?: "",
                    isAddedToDay = document.getBoolean("isAddedToDay") ?: false
                )
                exercises.add(exercise)
            }
            allExercises = exercises // Tutaj przypisujesz ćwiczenia do listy allExercises

            Log.d("SelectExerciseActivity", "Fetched exercises for category $category: $exercises")

            // Zamiast wywoływania `displayExercises()`, dodaj ćwiczenia do adaptera ćwiczeń
            exerciseAdapter.submitList(exercises)
            // Ukryj RecyclerView kategorii
            categoryList.visibility = View.GONE
            // Pokaż RecyclerView ćwiczeń
            exerciseList.visibility = View.VISIBLE
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // Tutaj pokazujemy pole EditText do wyszukiwania
                val searchField = findViewById<EditText>(R.id.search_exercise_input)
                searchField.visibility = View.VISIBLE
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }





    private fun displayExercises(exercises: List<Exercise>) {
        exerciseAdapter.submitList(exercises)
        Log.d(
            "SelectExerciseActivity",
            "Added ${exerciseAdapter.itemCount} exercises to the adapter"
        )


        val categories = listOf(
            "Back",
            "Biceps",
            "Chest",
            "Forearm",
            "Leg",
            "Shoulder",
            "Triceps"
        ) // Lista kategorii

        val exercisesPerCategory = mutableMapOf<String, List<Exercise>>()


        for (category in categories) {
            firestore.collection(category).get().addOnSuccessListener { documents ->
                val exercises = mutableListOf<Exercise>()
                for (document in documents) {
                    val exercise = Exercise(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        descriptionFile = document.getString("description") ?: "",
                        videoFile = document.getString("videoFile") ?: "",
                        isAddedToDay = document.getBoolean("isAddedToDay") ?: false
                    )

                    exercises.add(exercise)
                }

                exercisesPerCategory[category] = exercises
                exerciseAdapter.submitList(exercisesPerCategory.values.flatten())
                Log.d(
                    "SelectExerciseActivity",
                    "Added ${exerciseAdapter.itemCount} exercises to the adapter"
                )
            }.addOnFailureListener { exception ->
                Log.d("SelectExerciseActivity", "Error getting documents: ", exception)
            }
        }

        exerciseList = findViewById(R.id.exercise_list)
        exerciseList.layoutManager = LinearLayoutManager(this)
        exerciseList.adapter = exerciseAdapter
    }


    override fun onExerciseClick(exercise: Exercise) {
        Log.d("SelectExerciseActivity", "onExerciseClick: Exercise clicked: ${exercise.name}")
        exercise.isAddedToDay = true
        val resultIntent = Intent()
        resultIntent.putExtra("selectedExercise", exercise)
        val dayTitle = intent.getStringExtra("Dzień")
        if (dayTitle != null) {
            resultIntent.putExtra("Dzień", dayTitle)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onTxtIconClick(exercise: Exercise) {
        Log.d(
            "SelectExerciseActivity",
            "TXT Icon clicked: ${exercise.name}, description file: ${exercise.descriptionFile}"
        )
        val storageRef = storage.reference.child("exercisesFiles2/${exercise.descriptionFile}")
        storageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
            val description = String(bytes)
            val alertDialog = AlertDialog.Builder(this)
                .setTitle(exercise.name)
                .setMessage(description)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            alertDialog.show()
        }.addOnFailureListener { exception ->
            Log.d("SelectExerciseActivity", "Failed to download text file: $exception")
        }
    }

    override fun onMp4IconClick(exercise: Exercise) {
        Log.d("SelectExerciseActivity", "MP4 Icon clicked: ${exercise.name}")
        openVideoViewActivity(exercise)
    }

    private fun openVideoViewActivity(exercise: Exercise) {
        val storageRef = storage.reference.child("exercisesFiles2/${exercise.videoFile}")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            val intent = Intent(this, VideoViewActivity::class.java)
            intent.putExtra("videoUrl", uri.toString())
            intent.putExtra("exerciseName", exercise.name)  // Dodaj nazwę ćwiczenia do intencji
            Log.d(
                "SelectExerciseActivity",
                "Starting VideoViewActivity with videoUrl: ${uri.toString()}"
            )
            startActivity(intent)
        }.addOnFailureListener { exception ->
            Log.d("SelectExerciseActivity", "Failed to download video file: $exception")
        }
    }


    override fun onCancelClick(exercise: Exercise) {
        Log.d("SelectExerciseActivity", "onCancelClick: Exercise canceled: ${exercise.name}")
        exercise.isAddedToDay = false
        val resultIntent = Intent()
        resultIntent.putExtra("removedExercise", exercise)
        val dayTitle = intent.getStringExtra("Dzień")
        if (dayTitle != null) {
            resultIntent.putExtra("Dzień", dayTitle)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onCategoryClick(category: String) {
        val englishCategory = categoryNamesMapReverse[category]
        if (englishCategory != null) {
            Log.d("SelectExerciseActivity", "Fetching exercises for category: $englishCategory")
            fetchExercisesForCategory(englishCategory)
        } else {
            Log.e("SelectExerciseActivity", "Nieznana kategoria: $category")
        }
    }

}