package com.example.gympassion

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage

class DayViewHolder2(itemView: View, private val listener: OnDayClickListener, private val isEditing: Boolean) :
    RecyclerView.ViewHolder(itemView) {

    private val addExerciseButton: ImageButton = itemView.findViewById(R.id.add_exercises_button)
    private val deleteDayButton: ImageButton = itemView.findViewById(R.id.delete_day_button)
    private var day: Day? = null
    val dayTitle: TextView = itemView.findViewById(R.id.day_title)
    val exercisesRecyclerView: RecyclerView = itemView.findViewById(R.id.exercises_recycler_view2)


    private val exercisesAdapter = createExercisesAdapter(isEditing)
    private fun createExercisesAdapter(isEditing: Boolean): ExercisesAdapter {
        val layoutId = if (isEditing) R.layout.exercise_edit_list_item else R.layout.exercise_list_item
        return ExercisesAdapter(object : OnExerciseClickListener {
            override fun onExerciseClick(exercise: Exercise) {
                Log.d("DayViewHolder", "Exercise clicked: ${exercise.name}")
            }

            override fun onCancelClick(exercise: Exercise) {
                handleExerciseCancellation(exercise)
            }
            override fun onTxtIconClick(exercise: Exercise) {
                val storageReferenceTxt = FirebaseStorage.getInstance()
                    .getReferenceFromUrl("gs://gympassion-179b2.appspot.com/exercisesFiles2/${exercise.descriptionFile}")
                storageReferenceTxt.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                    val description = String(bytes)
                    val alertDialog = AlertDialog.Builder(itemView.context)
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
                val storageReferenceMp4 = FirebaseStorage.getInstance()
                    .getReferenceFromUrl("gs://gympassion-179b2.appspot.com/exercisesFiles2/${exercise.videoFile}")

                storageReferenceMp4.downloadUrl.addOnSuccessListener { uri ->
                    val intent = Intent(
                        itemView.context,
                        VideoViewActivity::class.java
                    )
                    intent.putExtra("videoUrl", uri.toString())
                    intent.putExtra("exerciseName", exercise.name) // przekazujemy nazwę ćwiczenia
                    Log.d("DayViewHolder", "Starting VideoViewActivity with videoUrl: ${uri.toString()}")
                    itemView.context.startActivity(intent)
                }
            }


        }, isEditing, layoutId)
    }

    private fun handleExerciseCancellation(exercise: Exercise) {
        val currentDay = day
        currentDay?.exercises?.let {
            if (it.contains(exercise)) {
                Log.d(
                    "DayViewHolder",
                    "Before cancelling exercise: isAddedToDay = ${exercise.isAddedToDay}"
                )
                it.remove(exercise)
                exercise.isAddedToDay = false
                Log.d(
                    "DayViewHolder",
                    "After cancelling exercise: isAddedToDay = ${exercise.isAddedToDay}"
                )
                Log.d(
                    "DayViewHolder",
                    "Cancelled exercise: ${exercise.name} from day: ${currentDay.title}"
                )
                exercisesAdapter.submitList(it.toList()) // Updated this line
            }
        }
    }

    init {
        exercisesRecyclerView.adapter = exercisesAdapter
        exercisesRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
    }

    fun bind(day: Day) {
        this.day = day
        dayTitle.text = day.title
        exercisesAdapter.submitList(day.exercises.toList())
        addExerciseButton.setOnClickListener {
            day?.let { listener.onDayClick(it) }
        }
        deleteDayButton.setOnClickListener {
            day?.let { listener.onDayDelete(it) }
        }
        dayTitle.setOnClickListener {
            day?.let { listener.onDayTitleClick(it) }
        }
    }



    fun addExercise(exercise: Exercise) {
        Log.d("DayViewHolder", "Before adding exercise: isAddedToDay = ${exercise.isAddedToDay}")
        val currentDay = day
        currentDay?.exercises?.let { exercises ->
            exercises.add(exercise)
            exercise.isAddedToDay = true
            exercisesAdapter.submitList(exercises.toList()) // Updated this line
        }
        Log.d("DayViewHolder", "After adding exercise: isAddedToDay = ${exercise.isAddedToDay}")
        Log.d("DayViewHolder", "Added exercise: ${exercise.name} to day: ${currentDay?.title}")
    }

}
