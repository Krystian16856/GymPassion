package com.example.gympassion

interface OnExerciseClickListener {
    fun onExerciseClick(exercise: Exercise)
    fun onCancelClick(exercise: Exercise)
    fun onTxtIconClick(exercise: Exercise)
    fun onMp4IconClick(exercise: Exercise)
}

