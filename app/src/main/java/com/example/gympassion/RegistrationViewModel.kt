package com.example.gympassion

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegistrationViewModel : ViewModel() {
    val password = MutableLiveData<String>()
    val passwordError = MutableLiveData<String>()

    fun passwordTextChanged(s: CharSequence) {
        password.value = s.toString()
        validatePassword()
    }

    private fun validatePassword() {
        if (password.value.isNullOrEmpty()) {
            passwordError.value = "Hasło nie może być puste"
        } else if (!password.value!!.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$".toRegex())) {
            passwordError.value = "Hasło musi mieć co najmniej 8 znaków, zawierać co najmniej jedną dużą literę, jedną małą literę, jedną cyfrę i jeden znak specjalny"
        } else {
            passwordError.value = ""
        }
    }
}
