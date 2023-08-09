package com.example.gympassion

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

open class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var translatedName: TextView = itemView.findViewById(R.id.translated_name_text)
    val pngIcon: ImageView = itemView.findViewById(R.id.png_file_icon)
    val webpIcon: ImageView = itemView.findViewById(R.id.webp_file_icon)
    val cancelIcon: ImageView = itemView.findViewById(R.id.cancel_icon)
}

class EditExerciseViewHolder(itemView: View) : ExerciseViewHolder(itemView) {
    val sets: EditText = itemView.findViewById(R.id.sets)
    val repetitions: EditText = itemView.findViewById(R.id.repetitions)
    val exerciseData: LinearLayout = itemView.findViewById(R.id.exercise_data2)
}


class ExercisesAdapter(private val listener: OnExerciseClickListener, private val isEditing: Boolean, private val layoutId: Int)
    : ListAdapter<Exercise, ExerciseViewHolder>(ExerciseComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(layoutId, parent, false)
        return if(isEditing) EditExerciseViewHolder(view) else ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = getItem(position)

        holder.translatedName.text = exercise.name
        holder.itemView.setOnClickListener {
            listener.onExerciseClick(exercise)
        }
        holder.cancelIcon.setOnClickListener {
            listener.onCancelClick(exercise)
        }

        if (exercise.isAddedToDay) {
            holder.cancelIcon.visibility = View.VISIBLE
        } else {
            holder.cancelIcon.visibility = View.GONE
        }

        holder.pngIcon.setOnClickListener {
            listener.onTxtIconClick(exercise)
        }

        holder.webpIcon.setOnClickListener {
            listener.onMp4IconClick(exercise)
        }

        if (holder is EditExerciseViewHolder && exercise.isAddedToDay) {
            holder.exerciseData.visibility = View.VISIBLE
            holder.sets.visibility = View.VISIBLE
            holder.repetitions.visibility = View.VISIBLE
            holder.sets.setText(exercise.sets.toString())
            holder.repetitions.setText(exercise.repetitions.toString())

            // Setup text watchers for the sets and repetitions EditTexts
            holder.sets.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    val input = s.toString()
                    if (input.isNotEmpty() && input.toInt() < 0) {
                        holder.sets.error = "Liczba nie może być ujemna"
                    } else {
                        exercise.sets = if (s.isEmpty()) 0 else s.toString().toInt()
                    }
                }
            })

            holder.repetitions.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    val input = s.toString()
                    if (input.isNotEmpty() && input.toInt() < 0) {
                        holder.repetitions.error = "Liczba nie może być ujemna"
                    } else {
                        exercise.repetitions = if (s.isEmpty()) 0 else s.toString().toInt()
                    }
                }
            })
        }
    }

    class ExerciseComparator : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.name == newItem.name
        }
    }
}
