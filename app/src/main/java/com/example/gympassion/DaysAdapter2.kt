package com.example.gympassion

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DaysAdapter2(private val listener: OnDayClickListener, private val layoutId: Int, private val isEditing: Boolean) :
    RecyclerView.Adapter<DayViewHolder2>() {

    private var days = mutableListOf<Day>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder2 {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(layoutId, parent, false)
        return DayViewHolder2(view, listener, isEditing)
    }

    override fun onBindViewHolder(holder: DayViewHolder2, position: Int) {
        val day = days[position]
        holder.bind(day)
        Log.d("DaysAdapter2", "Binding day at position $position with title ${day.title}") // Zmienione na DaysAdapter2
    }

    override fun getItemCount(): Int = days.size

    fun setDays(days: List<Day>) {
        this.days.clear()
        this.days.addAll(days)
        notifyDataSetChanged()
        Log.d("DaysAdapter2", "Set ${days.size} days")
    }

    fun addDay(day: Day) {
        this.days.add(day)
        notifyDataSetChanged()
        Log.d("DaysAdapter2", "Added day with title ${day.title}")
    }

    fun removeDay(day: Day) {
        this.days.remove(day)
        notifyDataSetChanged()
        // Log removal of day
        Log.d("DaysAdapter2", "Removed day with title ${day.title}")
    }


    fun getDays(): List<Day> {
        return this.days
    }
}
