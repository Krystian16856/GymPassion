package com.example.gympassion

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import soup.neumorphism.NeumorphButton

class CategoriesAdapter(
    private val context: Context,
    private val itemLayout: Int,
    private val listener: OnCategoryClickListener
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    private var categories = emptyList<String>()

    private val categoryNamesMap = mapOf(
        "Back" to "Plecy",
        "Biceps" to "Biceps",
        "Chest" to "Klatka piersiowa",
        "Forearm" to "Przedramię",
        "Leg" to "Nogi",
        "Shoulder" to "Barki",
        "Triceps" to "Triceps"
    )

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: NeumorphButton = itemView.findViewById(R.id.category_name_text)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemLayout, parent, false)
        return CategoryViewHolder(view)
    }

    override fun getItemCount(): Int = categories.size

    interface OnCategoryClickListener {
        fun onCategoryClick(category: String)
    }


    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val categoryEnglish = categories[position]
        val categoryPolish = categoryNamesMap[categoryEnglish] ?: categoryEnglish
        holder.categoryName.text = categoryPolish
        holder.categoryName.setOnClickListener {
            listener.onCategoryClick(categoryEnglish)
        }
    }




    fun submitList(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
