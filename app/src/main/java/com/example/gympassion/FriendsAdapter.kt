package com.example.gympassion

import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

class FriendsAdapter(var friendIds: MutableList<String>) : RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val nicknameTextView: TextView = view.findViewById(R.id.nicknameTextView)
        val lastActiveTextView: TextView = view.findViewById(R.id.lastActiveTextView)
        val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        val statusImageView: ImageView = view.findViewById(R.id.statusImageView)
        val messageImageView: ImageView = view.findViewById(R.id.messageImageView)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("FriendsAdapter", "Creating view holder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.friend_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friendId = friendIds[position]

        // Pobierz szczegóły przyjaciela z Firestore
        FirebaseFirestore.getInstance().collection("users").document(friendId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d("FriendsAdapter", "Loaded document: $document")
                    val friend = Friend(
                        name = document.getString("name") ?: "",
                        nickname = document.getString("nickname") ?: "",
                        isOnline = document.getBoolean("isOnline"),
                        userId = friendId,
                        lastOnlineTimestamp = document.getLong("lastOnlineTimestamp") ?: 0L
                    )
                    if (friend != null) {
                        Log.d("FriendsAdapter", "Binding view holder for friend: ${friend.nickname}, isOnline: ${friend.isOnline}")
                        friend.userId = document.id
                        Log.d("FriendsAdapter", "Loaded friend: $friend")
                        holder.nameTextView.text = friend.name
                        holder.nicknameTextView.text = friend.nickname

                        // Obliczamy różnicę czasu od ostatniej aktywności użytkownika
                        val currentTime = System.currentTimeMillis() /1000
                        val timeDifference = currentTime - friend.lastOnlineTimestamp

                        // Ustawiamy tekst i widoczność lastActiveTextView
                        if (friend.isOnline == true) {
                            holder.lastActiveTextView.visibility = View.GONE
                        } else {
                            holder.lastActiveTextView.text = "(${formatTimeDifference(timeDifference)})"
                            holder.lastActiveTextView.visibility = View.VISIBLE
                        }

                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                        if (currentUserId != null) {
                            listenForNewMessages(currentUserId, friendId, holder)
                        }

                        // Utwórz referencję do obrazu profilowego
                        val storageReference = FirebaseStorage.getInstance().reference
                        val profileImageRef = storageReference.child("profileImages/${friend.userId}")
                        Log.d("FriendsAdapter", "Trying to load profile image from: profileImages/${friend.userId}")

                        // Pobierz URL obrazu
                        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                            // Uri to URL obrazu, który możemy załadować za pomocą Glide
                            Glide.with(holder.itemView)
                                .load(uri)
                                .listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                                    override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                        Log.e("FriendsAdapter", "Load failed", e)
                                        return false // let Glide handle the rest
                                    }

                                    override fun onResourceReady(resource: Drawable?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean): Boolean {
                                        return false // let Glide handle the rest
                                    }
                                })
                                .placeholder(R.drawable.user)
                                .error(R.drawable.user)
                                .circleCrop()
                                .into(holder.profileImageView)

                        }.addOnFailureListener {
                            // Obsłuż sytuację, gdy pobranie URL nie powiedzie się, na przykład wyświetl log
                            Log.e("FriendsAdapter", "Failed to get profile image URL", it)
                            // Załaduj domyślne zdjęcie
                            Glide.with(holder.itemView)
                                .load(R.drawable.user) // Zakładając, że 'user' to domyślne zdjęcie w zasobach twojej aplikacji
                                .into(holder.profileImageView)
                        }

                        // Aktualizuj status ImageView w zależności od tego, czy użytkownik jest online
                        if (friend.isOnline == true) {
                            holder.statusImageView.setImageResource(R.drawable.online_status_icon)
                        } else {
                            holder.statusImageView.setImageResource(R.drawable.offline_status_icon)
                        }


                        // Dodajemy kod, który otwiera UserProfileActivity3 po kliknięciu na element listy
                        holder.itemView.setOnClickListener {
                            val context = holder.itemView.context
                            val intent = Intent(context, UserProfileActivity3::class.java)
                            intent.putExtra("userId", friendId)
                            context.startActivity(intent)
                        }

                        // Dodajemy kod, który otwiera ChatActivity po kliknięciu na ikonę wiadomości
                        holder.messageImageView.setOnClickListener {
                            val context = holder.itemView.context
                            val intent = Intent(context, ChatActivity::class.java)
                            intent.putExtra("userId", friendId)
                            context.startActivity(intent)
                            holder.messageImageView.setImageResource(R.drawable.message) // aktualizacja ikony wiadomości
                        }


                        // Nasłuchuj zmian w dokumentach wiadomości dla użytkownika
                        FirebaseFirestore.getInstance().collection("messages").document(friendId)
                            .addSnapshotListener { snapshot, e ->
                                if (e != null) {
                                    Log.w("FriendsAdapter", "Listen failed.", e)
                                    return@addSnapshotListener
                                }

                                if (snapshot != null && snapshot.exists()) {
                                    Log.d("FriendsAdapter", "Current data: ${snapshot.data}")
                                    // Jeśli wykryjemy nową wiadomość, zmieniamy ikonę na newmessage.png
                                    holder.messageImageView.setImageResource(R.drawable.newmessage)
                                } else {
                                    Log.d("FriendsAdapter", "Current data: null")
                                    // Jeśli nie ma nowej wiadomości, używamy domyślnej ikony wiadomości
                                    holder.messageImageView.setImageResource(R.drawable.message)
                                }
                                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                                if (currentUserId != null) {
                                    loadLastMessage(currentUserId, friendId, holder)
                                }
                            }
                    }
                }
            }
    }



    override fun getItemCount(): Int {
        Log.d("FriendsAdapter", "Getting item count: ${friendIds.size}")
        return friendIds.size
    }

    private fun getChatId(userId1: String, userId2: String): String {
        val userIds = listOf(userId1, userId2).sorted()
        return userIds.joinToString("_")
    }

    private fun listenForNewMessages(currentUserId: String, friendId: String, holder: ViewHolder) {
        val chatId = getChatId(currentUserId, friendId)
        Log.d("FriendsAdapter", "Listening for new messages between $currentUserId and $friendId")

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FriendsAdapter", "Listen for new messages failed.", e)
                    return@addSnapshotListener
                }

                for (documentChange in snapshots!!.documentChanges) {
                    when (documentChange.type) {
                        DocumentChange.Type.ADDED -> {
                            val newMessage = documentChange.document.toObject(Message::class.java)
                            if (!newMessage.readBy.contains(currentUserId)) {
                                holder.messageImageView.setImageResource(R.drawable.newmessage)
                            }
                        }
                        else -> {
                            // handle other types if needed
                        }
                    }
                }
            }
    }


    private fun loadLastMessage(currentUserId: String, friendId: String, holder: ViewHolder) {
        val chatId = getChatId(currentUserId, friendId)
        Log.d("FriendsAdapter", "Loading last message between $currentUserId and $friendId")

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FriendsAdapter", "Listen for last message failed.", e)
                    return@addSnapshotListener
                }

                for (document in snapshots!!.documents) {
                    val readBy = document.get("readBy") as? List<String> // rzutujemy na List<String>
                    if (readBy?.contains(currentUserId) == false) { // jeśli currentUserId nie jest na liście
                        holder.messageImageView.setImageResource(R.drawable.newmessage)
                    } else {
                        holder.messageImageView.setImageResource(R.drawable.message)
                    }
                }
            }
    }


    private fun formatTimeDifference(timeDifferenceInSeconds: Long): String {
        val minutes = timeDifferenceInSeconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30

        Log.d("FriendsAdapter", "formatTimeDifference: minutes=$minutes, hours=$hours, days=$days, weeks=$weeks, months=$months")


        return when {
            minutes < 60 -> "$minutes min temu"
            hours < 24 -> "$hours godź temu"
            days < 7 -> "$days dni temu"
            weeks < 4 -> "$weeks tyg temu"
            else -> "$months mieś temu"
        }
    }
}

data class Friend(
    val name: String = "",
    val nickname: String = "",
    val isOnline: Boolean? = null,
    val lastOnlineTimestamp: Long = 0L,
    var userId: String = ""
)

data class Message(
    val text: String = "",
    val senderId: String = "",
    val readBy: List<String> = emptyList(),
    // dodaj inne pola, jeśli są potrzebne
)



