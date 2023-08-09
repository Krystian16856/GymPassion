package com.example.gympassion

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class AddFriendActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchResultsAdapter: SearchResultsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        searchView = findViewById(R.id.searchView)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsAdapter = SearchResultsAdapter(mutableListOf())
        searchResultsRecyclerView.adapter = searchResultsAdapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Możesz również aktualizować wyniki wyszukiwania w czasie rzeczywistym, gdy użytkownik wpisuje tekst
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        searchResultsAdapter.notifyDataSetChanged()
    }

    private fun searchUsers(query: String?) {
        if (query != null && query.isNotEmpty()) {
            val formattedQuery = query.capitalize()
            val db = FirebaseFirestore.getInstance()

            // Pobierz listę identyfikatorów znajomych
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            db.collection("users").document(currentUserId!!).get().addOnSuccessListener { document ->
                val friendsField = document.get("friends")
                val friendIds = if (friendsField != null) {
                    friendsField as List<String>
                } else {
                    listOf()
                }

                val nicknameQuery = db.collection("users").whereEqualTo("nickname", query)
                val nameQuery = db.collection("users").whereEqualTo("name", formattedQuery)

                val combinedResults = mutableListOf<SearchUser>()

                nicknameQuery.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documents = task.result?.documents
                        if (documents != null) {
                            val searchResults = getSearchResults(documents, friendIds)
                            combinedResults.addAll(searchResults)
                        }
                    } else {
                        Log.e("AddFriendActivity", "Failed to search users by nickname", task.exception)
                    }

                    Log.d("AddFriendActivity", "Received ${combinedResults.size} results from nickname query")

                    nameQuery.get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val documents = task.result?.documents
                            if (documents != null) {
                                val searchResults = getSearchResults(documents, friendIds)
                                combinedResults.addAll(searchResults)
                            }
                        } else {
                            Log.e("AddFriendActivity", "Failed to search users by name", task.exception)
                        }

                        Log.d("AddFriendActivity", "Received ${combinedResults.size} results from name query")

                        updateSearchResultsRecyclerView(combinedResults)
                    }
                }
            }
        }
    }

    private fun getSearchResults(documents: List<DocumentSnapshot>, friendIds: List<String>): List<SearchUser> {
        return documents.mapNotNull { document ->
            // Przeskocz dokument, jeśli jest to bieżący użytkownik lub już jest znajomym
            if (document.id == FirebaseAuth.getInstance().currentUser?.uid || document.id in friendIds) {
                null
            } else {
                SearchUser(
                    name = document.getString("name") ?: "",
                    nickname = document.getString("nickname") ?: "",
                    isOnline = document.getBoolean("isOnline") ?: false,
                    userId = document.id,
                    lastOnlineTimestamp = document.getLong("lastOnlineTimestamp") ?: 0L / 1000
                )
            }
        }
    }


    private fun updateSearchResultsRecyclerView(searchResults: List<SearchUser>) {
        Log.d("AddFriendActivity", "Updating search results RecyclerView with ${searchResults.size} results")
        (searchResultsRecyclerView.adapter as SearchResultsAdapter).updateSearchResults(searchResults)
        searchResultsRecyclerView.invalidate()

        if (searchResults.isEmpty()) {
            Toast.makeText(this, "Nie znaleziono takiego użytkownika", Toast.LENGTH_SHORT).show()
        }
    }
}
