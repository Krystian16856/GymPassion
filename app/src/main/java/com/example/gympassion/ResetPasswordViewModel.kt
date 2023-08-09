package com.example.gympassion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ResetPasswordViewModel(private val auth: FirebaseAuth) : ViewModel() {

    sealed class ResetPasswordResult {
        object Success : ResetPasswordResult()
        class Failure(val exception: Exception) : ResetPasswordResult()
    }

    fun resetPassword(email: String, onResult: (ResetPasswordResult) -> Unit) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    auth.sendPasswordResetEmail(email).await()
                }
                onResult(ResetPasswordResult.Success)
            } catch (e: Exception) {
                onResult(ResetPasswordResult.Failure(e))
            }
        }
    }
}
