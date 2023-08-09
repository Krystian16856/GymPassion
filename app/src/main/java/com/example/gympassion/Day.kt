package com.example.gympassion

import java.io.Serializable
import java.util.UUID

data class Day(
    var title: String = "",
    var exerciseIds: MutableList<String> = mutableListOf(),
    var exercises: MutableList<Exercise> = mutableListOf(),
    var dayId: String = UUID.randomUUID().toString()
) : Serializable {

    fun addExercise(exercise: Exercise) {
        this.exercises.add(exercise)
        this.exerciseIds.add(exercise.id)
    }

    fun removeExercise(exercise: Exercise) {
        val index = exercises.indexOf(exercise)
        if (index != -1) {
            exercises.removeAt(index)
            exerciseIds.removeAt(index)
        }
    }
}
