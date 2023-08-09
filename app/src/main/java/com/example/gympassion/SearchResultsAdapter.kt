package com.example.gympassion

import android.animation.Animator
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SearchResultsAdapter(private val searchResults: MutableList<SearchUser>) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val nicknameTextView: TextView = view.findViewById(R.id.nicknameTextView)
        val lastActiveTextView: TextView = view.findViewById(R.id.lastActiveTextView)
        val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        val statusImageView: ImageView = view.findViewById(R.id.statusImageView)
        val addFriendImageView: ImageView = view.findViewById(R.id.addFriendImageView)
        val sentInviteAnimationView: LottieAnimationView = view.findViewById(R.id.sentInviteAnimationView)

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("SearchResultsAdapter", "Creating view holder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_result_item, parent, false)
        return ViewHolder(view)
    }




    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("SearchResultsAdapter", "Binding view holder for position $position")
        val user = searchResults[position]
        holder.nameTextView.text = user.name
        holder.nicknameTextView.text = user.nickname

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, UserProfileActivity::class.java)
            intent.putExtra("userId", user.userId)
            holder.itemView.context.startActivity(intent)
        }

        // Resetuj ikonę i OnClickListener na addFriendImageView na początku
        holder.addFriendImageView.setImageResource(R.drawable.addfriend)
        holder.addFriendImageView.setOnClickListener(null)

        val senderId = FirebaseAuth.getInstance().currentUser?.uid
        val receiverId = user.userId // userId użytkownika, którego profil jest przeglądany

        if (senderId != null) {
            // Sprawdź, czy istnieje już zaproszenie wysłane przez zalogowanego użytkownika
            FirebaseFirestore.getInstance().collection("invitations")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("status", "pending") // Dodano sprawdzanie statusu
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Nie znaleziono zaproszenia, ustaw ikonę na adduser.png
                        holder.addFriendImageView.setImageResource(R.drawable.addfriend)
                        // Ustaw OnClickListener, który pozwala wysłać zaproszenie
                        holder.addFriendImageView.setOnClickListener {
                            // Sprawdź, czy istnieje już zaproszenie wysłane przez zalogowanego użytkownika
                            FirebaseFirestore.getInstance().collection("invitations")
                                .whereEqualTo("senderId", senderId)
                                .whereEqualTo("receiverId", receiverId)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (documents.isEmpty) {
                                        // Nie znaleziono zaproszenia, sprawdź, czy istnieje zaproszenie wysłane przez drugiego użytkownika
                                        FirebaseFirestore.getInstance().collection("invitations")
                                            .whereEqualTo("senderId", receiverId)
                                            .whereEqualTo("receiverId", senderId)
                                            .get()
                                            .addOnSuccessListener { documents ->
                                                if (documents.isEmpty) {
                                                    val invitationId = if (senderId < receiverId) {
                                                        "${senderId}_${receiverId}"
                                                    } else {
                                                        "${receiverId}_${senderId}"
                                                    }

                                                    val invitation = hashMapOf(
                                                        "senderId" to senderId,
                                                        "receiverId" to receiverId,
                                                        "status" to "pending"
                                                    )

                                                    FirebaseFirestore.getInstance().collection("invitations")
                                                        .document(invitationId) // Dodajemy identyfikator dokumentu
                                                        .set(invitation) // Zmieniamy 'add' na 'set'
                                                        .addOnSuccessListener {
                                                            Log.d(
                                                                "SearchResultsAdapter",
                                                                "Invitation sent with ID: $invitationId"
                                                            )
                                                            // Uruchom animację i zmień ikonę addFriendImageView na sentinvite.png
                                                            holder.sentInviteAnimationView.visibility = View.VISIBLE
                                                            holder.sentInviteAnimationView.playAnimation()
                                                            holder.addFriendImageView.setImageResource(R.drawable.sentinvite)

                                                            // Ustaw nowy OnClickListener dla addFriendImageView, który pokazuje Toast z komunikatem
                                                            holder.addFriendImageView.setOnClickListener {
                                                                Toast.makeText(it.context, "Oczekuje na zaakceptowanie zaproszenie", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.w("SearchResultsAdapter", "Error adding invitation", e)
                                                        }
                                                } else {
                                                    // Zaproszenie już istnieje, pokaż komunikat
                                                    Toast.makeText(it.context, "Zaproszenie już zostało wysłane", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.w("SearchResultsAdapter", "Error checking for existing invitations", e)
                                            }
                                    } else {
                                        // Zaproszenie już istnieje, pokaż komunikat
                                        Toast.makeText(it.context, "Zaproszenie już zostało wysłane", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.w("SearchResultsAdapter", "Error checking for existing invitations", e)
                                }
                        }
                    } else {
                        // Zaproszenie istnieje i oczekuje na zaakceptowanie, ustaw ikonę na sentinvite.png
                        holder.addFriendImageView.setImageResource(R.drawable.sentinvite)
                        // Ustaw OnClickListener, który wyświetla Toast
                        holder.addFriendImageView.setOnClickListener {
                            Toast.makeText(it.context, "Oczekuje na zaakceptowanie zaproszenie", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("SearchResultsAdapter", "Error checking for existing invitations", e)
                }
        }

        holder.sentInviteAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                // Zignoruj, nie potrzebujemy tego teraz
            }

            override fun onAnimationEnd(animation: Animator) {
                // Ukryj animację po jej zakończeniu
                holder.sentInviteAnimationView.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
                // Zignoruj, nie potrzebujemy tego teraz
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Zignoruj, nie potrzebujemy tego teraz
            }
        })

        // Obliczamy różnicę czasu od ostatniej aktywności użytkownika
        val currentTime = System.currentTimeMillis() / 1000
        val timeDifference = currentTime - user.lastOnlineTimestamp

        // Ustawiamy tekst i widoczność lastActiveTextView
        if (user.isOnline) {
            holder.lastActiveTextView.visibility = View.GONE
        } else {
            holder.lastActiveTextView.text = "(${formatTimeDifference(timeDifference)})"
            holder.lastActiveTextView.visibility = View.VISIBLE
        }

        // Utwórz referencję do obrazu profilowego
        val storageReference = FirebaseStorage.getInstance().reference
        val profileImageRef = storageReference.child("profileImages/${user.userId}")

        // Pobierz URL obrazu
        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
            // Uri to URL obrazu, który możemy załadować za pomocą Glide
            Glide.with(holder.itemView)
                .load(uri)
                .placeholder(R.drawable.user) // obraz, który zostanie wyświetlony, zanim główny obraz zostanie załadowany
                .error(R.drawable.user) // obraz, który zostanie wyświetlony, jeśli wystąpi błąd podczas ładowania głównego obrazu
                .circleCrop() // zaokrągla obraz
                .into(holder.profileImageView) // ImageView, do którego obraz ma zostać załadowany
        }.addOnFailureListener {
            // Obsłuż sytuację, gdy pobranie URL nie powiedzie się, na przykład wyświetl log
            Log.e("SearchResultsAdapter", "Failed to get profile image URL", it)
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


    override fun getItemCount(): Int {
        Log.d("SearchResultsAdapter", "Item count requested. Current count: ${searchResults.size}")
        return searchResults.size
    }


    fun updateSearchResults(newSearchResults: List<SearchUser>) {
        Log.d("SearchResultsAdapter", "Updating search results. New count: ${newSearchResults.size}")
        val oldSize = searchResults.size
        searchResults.clear()
        notifyItemRangeRemoved(0, oldSize)
        searchResults.addAll(newSearchResults)
        notifyItemRangeInserted(0, newSearchResults.size)
    }

    fun formatTimeDifference(timeDifferenceInSeconds: Long): String {
        val minutes = timeDifferenceInSeconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30

        return when {
            minutes < 60 -> "$minutes min temu"
            hours < 24 -> "$hours godź temu"
            days < 7 -> "$days dni temu"
            weeks < 4 -> "$weeks tyg temu"
            else -> "$months mieś temu"
        }
    }


}

data class SearchUser(
    val name: String = "",
    val nickname: String = "",
    val isOnline: Boolean = false,
    var userId: String = "",
    val lastOnlineTimestamp: Long = 0L
)

