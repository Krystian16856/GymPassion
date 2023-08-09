package com.example.gympassion

import java.io.Serializable

data class Exercise(
    val id: String = "",
    val name: String = "",
    val descriptionFile: String = "",
    val videoFile: String = "",
    var isAddedToDay: Boolean = false,
    var sets: Int = 0,
    var repetitions: Int = 0,
    val category: String = ""
) : Serializable {
    val setsAndReps: String
        get() = "$sets series x $repetitions repetitions"

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "descriptionFile" to descriptionFile,
            "videoFile" to videoFile,
            "isAddedToDay" to isAddedToDay,
            "sets" to sets,
            "repetitions" to repetitions,
            "category" to category
        )
    }
}
