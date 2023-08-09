package com.example.gympassion

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendsActivity : AppCompatActivity() {

    private lateinit var addFriendButton: FloatingActionButton
    private lateinit var invitationsButton: FloatingActionButton
    private lateinit var friendsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)
        addFriendButton = findViewById(R.id.addFriendButton)
        invitationsButton = findViewById(R.id.invitationsButton)
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView)

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        invitationsButton.setOnClickListener {
            // Otwórz aktywność z zaproszeniami do przyjaciół
            val intent = Intent(this, InvitationsActivity::class.java)
            startActivity(intent)
        }
        addFriendButton.setOnClickListener {
            // Otwórz aktywność wyszukiwania przyjaciół
            val intent = Intent(this, AddFriendActivity::class.java)
            startActivity(intent)
        }

        // Dodaj ten log
        Log.d("FriendsActivity", "RecyclerView initialized: $friendsRecyclerView")


        friendsRecyclerView.adapter = FriendsAdapter(mutableListOf())

        // Dodaj ten log
        Log.d("FriendsActivity", "Adapter set: ${friendsRecyclerView.adapter}")

        friendsRecyclerView.layoutManager = LinearLayoutManager(this)
        // Pobierz listę przyjaciół z Firestore
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            checkInvitations(userId)
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(userId)
            docRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.toObject(User::class.java)
                    if (user != null) {
                        Log.d("FriendsActivity", "User: $user")
                        Log.d("FriendsActivity", "Friends: ${user.friends}")
                        (friendsRecyclerView.adapter as FriendsAdapter).friendIds = user.friends


                        // Dodaj ten log
                        Log.d("FriendsActivity", "Updated friend IDs in adapter: ${(friendsRecyclerView.adapter as FriendsAdapter).friendIds}")

                        (friendsRecyclerView.adapter as FriendsAdapter).notifyDataSetChanged()
                        friendsRecyclerView.postDelayed({
                            Log.d("FriendsActivity", "Number of child views in RecyclerView: ${friendsRecyclerView.childCount}")
                        }, 1000)

                        // Dodaj ten log
                        Log.d("FriendsActivity", "Notified adapter of data change")

                        friendsRecyclerView.post {
                            Log.d("FriendsActivity", "RecyclerView dimensions: ${friendsRecyclerView.width}x${friendsRecyclerView.height}")
                        }



                    }
                } else {
                    Log.e("FriendsActivity", "Failed to fetch friends list", task.exception)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Pobierz listę przyjaciół z Firestore
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("users").document(userId)
            docRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.toObject(User::class.java)
                    if (user != null) {
                        Log.d("FriendsActivity", "User: $user")
                        Log.d("FriendsActivity", "Friends: ${user.friends}")
                        (friendsRecyclerView.adapter as FriendsAdapter).friendIds = user.friends
                        (friendsRecyclerView.adapter as FriendsAdapter).notifyDataSetChanged()
                    }
                } else {
                    Log.e("FriendsActivity", "Failed to fetch friends list", task.exception)
                }
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }






    private fun checkInvitations(userId: String) {
        // Sprawdź, czy istnieje zaproszenie do przyjaciół dla tego użytkownika
        FirebaseFirestore.getInstance().collection("invitations")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Znaleziono zaproszenie do przyjaciół, ustaw przycisk zaproszeń na widoczny
                    invitationsButton.visibility = View.VISIBLE
                } else {
                    // Nie znaleziono zaproszenia do przyjaciół, ustaw przycisk zaproszeń na niewidoczny
                    invitationsButton.visibility = View.GONE
                }
            }
    }
}
data class User(
    val name: String = "",
    val nickname: String = "",
    val friends: MutableList<String> = mutableListOf()
)



