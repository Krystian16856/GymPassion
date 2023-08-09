package com.example.gympassion

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class InvitationsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var invitationsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invitations)

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        invitationsRecyclerView = findViewById(R.id.invitationsRecyclerView)
        invitationsRecyclerView.layoutManager = LinearLayoutManager(this)
        invitationsRecyclerView.adapter = InvitationsAdapter(listOf())

        loadInvitations()
    }

    override fun onBackPressed() {
        val intent = Intent(this, FriendsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
        finish()
    }

    private fun loadInvitations() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            db.collection("invitations")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val invitations = querySnapshot.documents.map { doc ->
                        val senderId = doc.getString("senderId")
                        val invitationId = doc.id
                        Invitation(senderId, invitationId)
                    }
                    (invitationsRecyclerView.adapter as InvitationsAdapter).invitations = invitations
                    invitationsRecyclerView.adapter?.notifyDataSetChanged()

                    // If the list of invitations is empty after loading, start DashboardActivity
                    if (invitations.isEmpty()) {
                        val intent = Intent(this@InvitationsActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("InvitationsActivity", "Error getting invitations", exception)
                }
        }
    }


    inner class InvitationsAdapter(var invitations: List<Invitation>) : RecyclerView.Adapter<InvitationsAdapter.InvitationViewHolder>() {

        inner class InvitationViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
            val nameTextView: TextView = view.findViewById(R.id.nameTextView)
            val nicknameTextView: TextView = view.findViewById(R.id.nicknameTextView)
            val addUserImageView: ImageView = view.findViewById(R.id.addUserImageView)
            val statusImageView: ImageView = view.findViewById(R.id.statusImageView)
            val rejectUserImageView: ImageView = view.findViewById(R.id.rejectUserImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_invitation, parent, false)
            return InvitationViewHolder(view)
        }


        override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
            val invitation = invitations[position]

            // Pobierz dane użytkownika
            db.collection("users").document(invitation.senderId!!)
                .get()
                .addOnSuccessListener { document ->
                    val user = SearchUser(
                        name = document.getString("name") ?: "",
                        nickname = document.getString("nickname") ?: "",
                        isOnline = document.getBoolean("isOnline") ?: false,
                        userId = document.id,
                        lastOnlineTimestamp = document.getLong("lastOnlineTimestamp") ?: 0L
                    )

                    if (user != null) {
                        // Ustawienie nazwy i pseudonimu
                        holder.nameTextView.text = user.name
                        holder.nicknameTextView.text = user.nickname
                        Log.d("InvitationsActivity", "User id: ${user.userId}")
                        Log.d("InvitationsActivity", "Loaded user with ID: ${user.userId}")

                        // Utwórz referencję do obrazu profilowego
                        val storageReference = FirebaseStorage.getInstance().reference
                        val profileImageRef = storageReference.child("profileImages/${user.userId}")
                        Log.d("InvitationsActivity", "Loading image from: profileImages/${user.userId}")

                        // Pobierz URL obrazu
                        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                            // Uri to URL obrazu, który możemy załadować za pomocą Glide
                            Glide.with(holder.itemView)
                                .load(uri)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Log.e("InvitationsActivity", "Error loading image", e)
                                        return false // let Glide handle the rest
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable?,
                                        model: Any?,
                                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                                        dataSource: com.bumptech.glide.load.DataSource?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        return false // let Glide handle the rest
                                    }
                                })
                                .placeholder(R.drawable.user)
                                .error(R.drawable.user)
                                .circleCrop()
                                .into(holder.profileImageView) // ImageView, do którego obraz ma zostać załadowany
                        }.addOnFailureListener {
                            // Obsłuż sytuację, gdy pobranie URL nie powiedzie się, na przykład wyświetl log
                            Log.e("InvitationsActivity", "Failed to get profile image URL", it)
                            // Załaduj domyślne zdjęcie
                            Glide.with(holder.itemView)
                                .load(R.drawable.user) // Zakładając, że 'user' to domyślne zdjęcie w zasobach twojej aplikacji
                                .into(holder.profileImageView)
                        }

                        // Aktualizuj status ImageView w zależności od tego, czy użytkownik jest online
                        if (user.isOnline) {
                            holder.statusImageView.setImageResource(R.drawable.online_status_icon)
                        } else {
                            holder.statusImageView.setImageResource(R.drawable.offline_status_icon)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("InvitationsActivity", "Error getting user", e)
                }

            holder.addUserImageView.setOnClickListener {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId != null) {
                    // Update current user's friend list
                    val currentUserRef = db.collection("users").document(currentUserId)
                    currentUserRef.update(
                        "friends",
                        FieldValue.arrayUnion(invitation.senderId)
                    )
                        .addOnSuccessListener {
                            Log.d(
                                "InvitationsActivity",
                                "Successfully updated current user's friends list"
                            )
                            // Update the sender's friend list
                            val senderUserRef = db.collection("users").document(invitation.senderId!!)
                            senderUserRef.update("friends", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener {
                                    Log.d(
                                        "InvitationsActivity",
                                        "Successfully updated sender's friends list"
                                    )
                                    // Delete invitation
                                    db.collection("invitations").document(invitation.invitationId)
                                        .delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(this@InvitationsActivity, "Zaproszenie przyjęte", Toast.LENGTH_SHORT).show()
                                            loadInvitations()
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.w("InvitationsActivity", "Error deleting invitation", exception)
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e(
                                        "InvitationsActivity",
                                        "Error updating sender's friends list",
                                        exception
                                    )
                                }
                        }
                        .addOnFailureListener { exception ->
                            Log.e(
                                "InvitationsActivity",
                                "Error updating current user's friends list",
                                exception
                            )
                        }
                }
            }

            holder.rejectUserImageView.setOnClickListener {
                // Usuń zaproszenie
                db.collection("invitations").document(invitation.invitationId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@InvitationsActivity, "Zaproszenie odrzucone", Toast.LENGTH_SHORT).show()
                        loadInvitations()
                    }
                    .addOnFailureListener { exception ->
                        Log.w("InvitationsActivity", "Błąd podczas odrzucania zaproszenia", exception)
                    }
            }


            holder.view.setOnClickListener {
                // View profile
                val intent = Intent(this@InvitationsActivity, UserProfileActivity2::class.java)
                intent.putExtra("userId", invitation.senderId)
                intent.putExtra("invitationId", invitation.invitationId) // Pass the invitationId
                startActivity(intent)
            }

        }


        override fun getItemCount(): Int {
            return invitations.size
        }
    }

    data class Invitation(val senderId: String?, val invitationId: String)
}
