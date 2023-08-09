package com.example.gympassion

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ChatActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var messagesRecyclerView: RecyclerView
    private var currentUserId: String? = null
    private lateinit var messageEditText: EditText
    private lateinit var sendMessageButton: ImageButton
    private var friendId: String? = null

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid
        db = FirebaseFirestore.getInstance()

        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        // Aktualizacja statusu online użytkownika
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid)
                .update("isOnline", true)
                .addOnSuccessListener {
                    Log.d("ChatActivity", "User online status updated to 'true'")
                }
                .addOnFailureListener { e ->
                    Log.w("ChatActivity", "Error updating user online status", e)
                }
        }


        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = ChatAdapter(listOf())

        friendId = intent.getStringExtra("userId")
        messageEditText = findViewById(R.id.messageEditText)
        sendMessageButton = findViewById(R.id.sendMessageButton)

        sendMessageButton.setOnClickListener {
            val currentUserId = auth.currentUser?.uid
            val friendId = intent.getStringExtra("userId")
            if (currentUserId != null && friendId != null) {
                sendMessage(currentUserId, friendId)
            }
        }


        val currentUserId = auth.currentUser?.uid
        val friendId = intent.getStringExtra("userId")

        // Locate the views
        val chatUserImageView: ImageView = findViewById(R.id.chatUserImageView)
        val chatUserNameTextView: TextView = findViewById(R.id.chatUserNameTextView)

        // Load the friend's profile image and name
        if (friendId != null) {
            db.collection("users").document(friendId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userName = document.getString("name") ?: ""
                        val userImage = document.getString("profileImageUrl")

                        chatUserNameTextView.text = userName

                        // Load profile image
                        val storageReference = FirebaseStorage.getInstance().reference
                        val profileImageRef = storageReference.child("profileImages/$friendId")

                        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                            Glide.with(this)
                                .load(uri)
                                .placeholder(R.drawable.user)
                                .error(R.drawable.user)
                                .circleCrop()
                                .into(chatUserImageView)
                        }.addOnFailureListener {
                            Glide.with(this)
                                .load(R.drawable.user)
                                .into(chatUserImageView)
                        }
                        chatUserImageView.setOnClickListener {
                            val intent = Intent(this, UserProfileActivity3::class.java)
                            intent.putExtra("userId", friendId)
                            startActivity(intent)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("ChatActivity", "Error getting user info", exception)
                }
        }

        if (currentUserId != null && friendId != null) {
            markMessagesAsRead(currentUserId, friendId)
            val chatId = getChatId(currentUserId, friendId) // Get the chat id
            db.collection("chats")
                .document(chatId)
                .set(mapOf("userIds" to listOf(currentUserId, friendId)))
                .addOnSuccessListener {
                    loadChat(currentUserId, friendId)
                }
                .addOnFailureListener { exception ->
                    Log.w("ChatActivity", "Error creating chat", exception)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        messageEditText.requestFocus()
        messageEditText.setOnTouchListener { v, event ->
            v.isFocusable = true
            v.isFocusableInTouchMode = true
            false
        }

    }

    override fun onBackPressed() {
        val intent = Intent(this, FriendsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
        finish()
    }




    private fun getChatId(userId1: String, userId2: String): String {
        val userIds = listOf(userId1, userId2).sorted()
        return userIds.joinToString("_")
    }

    private fun sendMessage(currentUserId: String, friendId: String) {
        val text = messageEditText.text.toString()

        if (text.isNotEmpty()) {
            val message = mapOf(
                "senderId" to currentUserId,
                "text" to text,
                "timestamp" to System.currentTimeMillis(),
                "readBy" to listOf(currentUserId)
            )

            Log.d("ChatActivity", "Sending message: $message from $currentUserId to $friendId")

            val chatId = getChatId(currentUserId, friendId)

            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener {
                    messageEditText.text.clear()
                }
                .addOnFailureListener { exception ->
                    Log.w("ChatActivity", "Error sending message", exception)
                }
        }
    }

        private fun markMessagesAsRead(currentUserId: String, friendId: String) {
            val chatId = getChatId(currentUserId, friendId)

            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        val readBy =
                            document.get("readBy") as? List<String> // rzutujemy na List<String>
                        if (readBy?.contains(currentUserId) == false) { // jeśli currentUserId nie jest na liście
                            // Dodajemy currentUserId do listy
                            val newReadBy = readBy + currentUserId
                            document.reference.update("readBy", newReadBy)
                        }
                    }
                }
        }


    private fun loadChat(currentUserId: String, friendId: String) {
        val chatId = getChatId(currentUserId, friendId) // Get the chat id
        Log.d("ChatActivity", "Loading chat between $currentUserId and $friendId")

        db.collection("chats")
            .document(chatId)
            .collection("messages") // Load the messages from the "messages" collection inside the chat document
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.w("ChatActivity", "Error getting chats", exception)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.map { doc ->
                    val senderId = doc.getString("senderId")
                    val messageText = doc.getString("text")
                    Message(senderId, messageText)
                }

                (messagesRecyclerView.adapter as ChatAdapter).messages = messages ?: listOf()
                messagesRecyclerView.adapter?.notifyDataSetChanged()

                // Scroll to the bottom of the list
                if (messages != null && messages.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }



    inner class ChatAdapter(var messages: List<Message>) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

        open inner class MessageViewHolder(val view: View) : RecyclerView.ViewHolder(view)

        inner class SentMessageViewHolder(view: View) : MessageViewHolder(view) {
            val sentMessageTextView: TextView = view.findViewById(R.id.sentMessageTextView)
            val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        }

        inner class ReceivedMessageViewHolder(view: View) : MessageViewHolder(view) {
            val receivedMessageTextView: TextView = view.findViewById(R.id.receivedMessageTextView)
            val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        }


        override fun getItemViewType(position: Int): Int {
            val message = messages[position]
            return if (message.senderId == currentUserId) {
                VIEW_TYPE_SENT
            } else {
                VIEW_TYPE_RECEIVED
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            return if (viewType == VIEW_TYPE_SENT) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_sent_message, parent, false)
                SentMessageViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_received_message, parent, false)
                ReceivedMessageViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            if (holder is SentMessageViewHolder && message.senderId == currentUserId) {
                holder.sentMessageTextView.text = message.text

                // Load profile image
                val storageReference = FirebaseStorage.getInstance().reference
                val profileImageRef = storageReference.child("profileImages/${message.senderId}")

                profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(holder.itemView)
                        .load(uri)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .circleCrop()
                        .into(holder.profileImageView)
                }.addOnFailureListener {
                    Glide.with(holder.itemView)
                        .load(R.drawable.user)
                        .into(holder.profileImageView)
                }
            } else if (holder is ReceivedMessageViewHolder && message.senderId != currentUserId) {
                holder.receivedMessageTextView.text = message.text

                // Load profile image
                val storageReference = FirebaseStorage.getInstance().reference
                val profileImageRef = storageReference.child("profileImages/${message.senderId}")

                profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(holder.itemView)
                        .load(uri)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .circleCrop()
                        .into(holder.profileImageView)
                }.addOnFailureListener {
                    Glide.with(holder.itemView)
                        .load(R.drawable.user)
                        .into(holder.profileImageView)
                }
            }
        }



        override fun getItemCount(): Int {
            return messages.size
        }
    }


    data class Message(val senderId: String?, val text: String?)
}