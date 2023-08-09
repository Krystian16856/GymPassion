package com.example.gympassion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MaxWeightsAdapter(private val maxWeightsData: List<MaxWeight>) :
    RecyclerView.Adapter<MaxWeightsAdapter.MaxWeightViewHolder>() {

    class MaxWeightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exerciseName: TextView = itemView.findViewById(R.id.exercise_name)
        val exerciseWeight: TextView = itemView.findViewById(R.id.exercise_weight)
        val exerciseDate: TextView = itemView.findViewById(R.id.exercise_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaxWeightViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.max_weight_item,
            parent,
            false
        )

        return MaxWeightViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MaxWeightViewHolder, position: Int) {
        val currentItem = maxWeightsData[position]

        holder.exerciseName.text = currentItem.name
        holder.exerciseWeight.text = "${currentItem.weight} ${if (currentItem.metric) "Kg" else "Lb"}"
        holder.exerciseDate.text = currentItem.date
    }

    override fun getItemCount() = maxWeightsData.size
}
data class MaxWeight(val name: String, val weight: Double, val date: String, val metric: Boolean)



