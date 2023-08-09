package com.example.gympassion

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DaysAdapter(private val listener: OnDayClickListener, private val layoutId: Int, private val isEditing: Boolean) :
    RecyclerView.Adapter<DayViewHolder>() {

    private var days = mutableListOf<Day>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(layoutId, parent, false)
        return DayViewHolder(view, listener, isEditing)

    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.bind(day)
        // Log bind action
        Log.d("DaysAdapter", "Binding day at position $position with title ${day.title}")
    }

    override fun getItemCount(): Int = days.size



    fun setDays(days: List<Day>) {
        this.days.clear()
        this.days.addAll(days)
        notifyDataSetChanged()
        // Log setting of days
        Log.d("DaysAdapter", "Set ${days.size} days")
    }

    fun addDay(day: Day) {
        this.days.add(day)
        notifyDataSetChanged()
        // Log adding of day
        Log.d("DaysAdapter", "Added day with title ${day.title}")
    }

    fun removeDay(day: Day) {
        this.days.remove(day)
        notifyDataSetChanged()
        // Log removal of day
        Log.d("DaysAdapter", "Removed day with title ${day.title}")
    }


    fun getDays(): List<Day> {
        return this.days
    }
}
