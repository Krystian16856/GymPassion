package com.example.gympassion

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.squareup.picasso.Picasso


class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()
        db = FirebaseFirestore.getInstance()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data["type"]

        when (type) {
            "message" -> handleChatNotification(remoteMessage)
            "invitation" -> handleInvitationNotification(remoteMessage)
            "workoutReminder" -> handleWorkoutReminder(remoteMessage)
            else -> Log.w("MyFirebaseMessagingService", "Unknown notification type received")
        }
    }

    private fun handleChatNotification(remoteMessage: RemoteMessage) {
        // Odbierz dane z powiadomienia
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]
        val imageUrl = remoteMessage.data["imageUrl"]?.let { Uri.parse(it) }
        val friendId = remoteMessage.data["friendId"]

        // Sprawdź wartość isOnline dla użytkownika w bazie danych Firebase
        db.collection("users").document(friendId!!).get()
            .addOnSuccessListener { document ->
                val isOnline = document.getBoolean("isOnline") ?: false
                if (!isOnline) {
                    // Aktualizuj wartość isOnline dla użytkownika
                    db.collection("users").document(friendId)
                        .update("isOnline", true)
                        .addOnSuccessListener {
                            // Zaktualizowano pomyślnie
                            // Teraz wyświetl powiadomienie
                            createNotification(title, body, imageUrl, friendId, "message")
                        }
                        .addOnFailureListener { exception ->
                            Log.w(
                                "MyFirebaseMessagingService",
                                "Error updating isOnline",
                                exception
                            )
                        }
                } else {
                    // Jeśli isOnline jest już true, po prostu wyświetl powiadomienie
                    createNotification(title, body, imageUrl, friendId, "message")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("MyFirebaseMessagingService", "Error getting user document", exception)
            }
    }

    private fun handleInvitationNotification(remoteMessage: RemoteMessage) {
        // Pobierasz dane z powiadomienia
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]
        val imageUrl = remoteMessage.data["imageUrl"]?.let { Uri.parse(it) }

        // Tworzysz intent, który otworzy InvitationsActivity po kliknięciu w powiadomienie
        val intent = Intent(this, InvitationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Teraz tworzysz powiadomienie z uzyskanymi danymi
        createNotification(
            title,
            body,
            imageUrl,
            null,
            "invitation"
        )// Gdzie 'null' to brak friendId dla tego typu powiadomienia
    }

    private fun handleWorkoutReminder(remoteMessage: RemoteMessage) {
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]

        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Teraz tworzysz powiadomienie z uzyskanymi danymi
        createNotification(title, body, null, null, "workoutReminder")
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Jeśli chcesz, możesz tutaj obsłużyć odświeżanie tokena FCM.
    }

    private fun createNotification(
        title: String?,
        body: String?,
        imageUrl: Uri?,
        friendId: String?,
        notificationType: String
    ) {
        // Utwórz intent, który otworzy ChatActivity (lub inny odpowiedni intent) po kliknięciu w powiadomienie
        val intent = if (friendId != null) {
            Intent(this, ChatActivity::class.java).apply {
                putExtra("userId", friendId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        } else {
            Intent(this, InvitationsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "chat_messages_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (imageUrl == null) {
            val defaultDrawable = when (notificationType) {
                "message", "invitation" -> resources.getDrawable(R.drawable.user, null)
                "workoutReminder" -> resources.getDrawable(R.drawable.training, null)
                else -> resources.getDrawable(
                    R.drawable.user,
                    null
                ) // Domyślny obrazek, jeśli żaden z powyższych typów nie pasuje
            }
            notificationBuilder.setLargeIcon((defaultDrawable as BitmapDrawable).bitmap)
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
        } else {
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .error(R.drawable.user)
                .circleCrop()
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("NotificationService", "Load failed", e)
                        return false // allow Glide's error handling to proceed
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("NotificationService", "Resource ready, dataSource: $dataSource")
                        return false // allow Glide's success handling to proceed
                    }
                })
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        Log.d(
                            "NotificationService",
                            "Resource ready, setting it to the notification"
                        )
                        notificationBuilder.setLargeIcon(resource)
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(
                            0 /* ID of notification */,
                            notificationBuilder.build()
                        )
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Log.d("NotificationService", "Load failed")
                        // Teraz, gdy obraz profilowy nie mógł zostać załadowany, używany jest obraz zastępczy
                        notificationBuilder.setLargeIcon((errorDrawable as BitmapDrawable).bitmap)
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(
                            0 /* ID of notification */,
                            notificationBuilder.build()
                        )
                    }
                })
        }
    }
}
