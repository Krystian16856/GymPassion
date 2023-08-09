package com.example.gympassion


data class WorkoutResult(
    val date: Long,
    val load: Float,
    val exerciseName: String,
    val previousWeekWeight: List<String>,
    val series: Int,
    val duration: String,
    val timestamp: Long
)



