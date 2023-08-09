package com.example.gympassion

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Picasso.get().setLoggingEnabled(true)

        val appLifecycleObserver = AppLifecycleObserver()

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }

    inner class AppLifecycleObserver : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onAppBackgrounded() {
            // App went to background
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val user = auth.currentUser
            if (user != null) {
                val currentTimestamp = System.currentTimeMillis() / 1000 // get current timestamp in seconds
                db.collection("users").document(user.uid)
                    .update(
                        mapOf(
                            "isOnline" to false,
                            "lastOnlineTimestamp" to currentTimestamp
                        )
                    )
                    .addOnSuccessListener {
                        Log.d("MainActivity", "User online status and last online timestamp updated")
                    }
                    .addOnFailureListener { e ->
                        Log.w("MainActivity", "Error updating user online status and last online timestamp", e)
                    }
            }
        }



        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onAppForegrounded() {
            // App went to foreground
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            val user = auth.currentUser
            if (user != null && !MainActivity.isInMainActivity) {
                db.collection("users").document(user.uid).get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val isLoggedIn = document.getBoolean("isLoggedIn") ?: false
                            if (isLoggedIn) {
                                db.collection("users").document(user.uid)
                                    .update("isOnline", true)
                                    .addOnSuccessListener {
                                        Log.d("MainActivity", "User online status updated to 'true'")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("MainActivity", "Error updating user online status", e)
                                    }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("MainActivity", "Error getting document", e)
                    }
            }
    }
    }

}
