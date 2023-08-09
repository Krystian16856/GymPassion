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

class ExerciseAdapter(
    private val listener: OnExerciseClickListener
) : ListAdapter<Exercise, ExerciseViewHolder>(ExerciseComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.exercise_list_item, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val exercise = getItem(position)

        holder.translatedName.text = exercise.name

        holder.itemView.setOnClickListener {
            listener.onExerciseClick(exercise)
        }

        holder.cancelIcon.setOnClickListener {
            Log.d("ExercisesAdapter", "Cancel icon clicked for exercise: ${exercise.name}")
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
