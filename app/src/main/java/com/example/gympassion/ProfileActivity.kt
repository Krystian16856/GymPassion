package com.example.gympassion

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import soup.neumorphism.NeumorphCardView
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.ExecutionException


class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var updateButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var editTextDescription: EditText
    private lateinit var textViewWorkoutMethod: TextView
    private var workoutMethod: String? = null
    private var profileImageUri: Uri? = null
    private var currentPhotoPath: String = ""

    companion object {
        const val REQUEST_TAKE_PHOTO = 1
    }

    private suspend fun hasAgeHeightAndWorkoutMethod(): Boolean {
        var hasData = false
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val document = Tasks.await(db.collection("users").document(userId).get())
                val user = document.data
                val height = user?.get("height") as? Long
                val birthDate = user?.get("birthDate") as? com.google.firebase.Timestamp
                val workoutMethod = user?.get("workoutMethod") as? String
                if (height != null && birthDate != null && workoutMethod != null) {
                    hasData = true
                }
            } catch (e: ExecutionException) {
                Toast.makeText(
                    this,
                    "Failed to load user data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: InterruptedException) {
                Toast.makeText(
                    this,
                    "Failed to load user data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return hasData
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        profileImageView = findViewById(R.id.profile_image)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


        val rootView = findViewById<View>(android.R.id.content)
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            rootView.setBackgroundResource(R.drawable.background_dark)
        } else {
            rootView.setBackgroundResource(R.drawable.background_gradient)
        }

        val profileImage: ImageView = findViewById(R.id.profile_image)
        profileImage.setOnClickListener {
            selectImage()
        }

        val editTextHeight: EditText = findViewById(R.id.editTextHeight)
        val editTextAge: EditText = findViewById(R.id.editTextAge)
        val textViewGym: TextView = findViewById(R.id.textViewGym)
        val textViewName: TextView = findViewById(R.id.textViewName)
        val remainingCharsTextView: TextView = findViewById(R.id.remainingCharsTextView)

        editTextDescription = findViewById(R.id.editTextDescription)

        editTextDescription.requestFocus()
        editTextDescription.setOnTouchListener { v, event ->
            v.isFocusable = true
            v.isFocusableInTouchMode = true
            false
        }

        editTextDescription.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.length > 500) {
                    s.delete(500, s.length)
                }
                val remaining = 500 - s.length
                remainingCharsTextView.text = "$remaining/500"
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}


        })


        val userId = auth.currentUser?.uid
        val profileImageView: ImageView = findViewById(R.id.profile_image)

        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$userId")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(profileImageView)
        }.addOnFailureListener {
            // W przypadku błędu, załaduj domyślne zdjęcie
            Glide.with(this)
                .load(R.drawable.user)  // Zakładając, że 'user' to domyślne zdjęcie w zasobach twojej aplikacji
                .into(profileImageView)
        }


        val gymCardView: CardView = findViewById(R.id.gymCardView)
        gymCardView.setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:0,0?q=gyms")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }


        editTextDescription.setOnClickListener {
            Log.d("ProfileActivity", "Description EditText clicked")
            editTextDescription.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextDescription, InputMethodManager.SHOW_IMPLICIT)
        }
        updateButton = findViewById(R.id.buttonUpdate)
        updateButton.setOnClickListener {
            val heightString = editTextHeight.text.toString()
            val height = heightString.replace(" cm", "").toInt()
            var gym = textViewGym.text.toString() // Nowe
            val description = editTextDescription.text.toString()

            // Sprawdzamy, czy wartość 'gym' jest pusta, a jeśli tak, to zapisujemy "Nie wybrano siłowni"
            if (gym.isEmpty()) {
                gym = "Nie wybrano siłowni"
            }

            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Tworzenie mapy danych do aktualizacji
                val data = mutableMapOf<String, Any>(
                    "height" to height,
                    "gym" to gym,
                    "description" to description
                )
                // Dodawanie wartości 'workoutMethod' do mapy, tylko jeśli nie jest null
                if (workoutMethod != null) {
                    data["workoutMethod"] = workoutMethod!!
                }

                db.collection("users")
                    .document(userId)
                    .update(data) // Aktualizacja danych w Firestore
                    .addOnSuccessListener {
                        Toast.makeText(this, "Dane zostały zaktualizowane.", Toast.LENGTH_SHORT)
                            .show()

                        val localUri = profileImageUri
                        if (localUri != null) {
                            val storageRef =
                                FirebaseStorage.getInstance().reference.child("profileImages/$userId")
                            val uploadTask = storageRef.putFile(localUri)
                            uploadTask.addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Zdjęcie zostało zapisane.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener { exception ->
                                Toast.makeText(
                                    this,
                                    "Nie udało się zapisać zdjęcia: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        finish()
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Nie udało się zaktualizować danych: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
        textViewWorkoutMethod = findViewById(R.id.textViewWorkoutMethod)
        textViewWorkoutMethod.setOnClickListener {
            showWorkoutMethodDialog()
        }


        checkUserData()




        if (userId != null) {
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.data
                    val username = user?.get("name") as? String
                    val height = user?.get("height") as? Long
                    val workoutMethod = user?.get("workoutMethod") as? String
                    Log.d("ProfileActivity", "Height from Firestore: $height")
                    val birthDate = user?.get("birthDate") as? com.google.firebase.Timestamp
                    val gym = user?.get("gym") as? String
                    val description = user?.get("description") as? String

                    textViewName.text = username
                    Log.d("ProfileActivity", "Setting height: ${height}")
                    editTextHeight.setText(if (height != null) "${height} cm" else "Brak danych")
                    textViewGym.text = gym // Nowe
                    Log.d("ProfileActivity", "Height set")
                    editTextAge.setText(
                        calculateAge(
                            birthDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())
                                ?.toLocalDate()
                        ).toString() + " lat"
                    )
                    editTextDescription.setText(description)
                    textViewWorkoutMethod.text = workoutMethod ?: "Brak danych"

                }

        }
    }

    override fun onResume() {
        super.onResume()
        editTextDescription.requestFocus()
        editTextDescription.setOnTouchListener { v, event ->
            v.isFocusable = true
            v.isFocusableInTouchMode = true
            false
        }
    }

    private fun checkUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val user = document.data
                    val height = user?.get("height") as? Long
                    val birthDate = user?.get("birthDate") as? com.google.firebase.Timestamp
                    val workoutMethod = user?.get("workoutMethod") as? String
                    if (height == null || birthDate == null || workoutMethod == null) {
                        val intent = Intent(this@ProfileActivity, AgeHeightActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Failed to load user data: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }


    private fun showWorkoutMethodDialog() {
        val workoutMethods = arrayOf("FBW", "Push Pull", "Push Pull Legs", "Split", "Trening obwodowy")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wybierz metodę treningu")

        val workoutMethodSpinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, workoutMethods)
        workoutMethodSpinner.adapter = adapter
        builder.setView(workoutMethodSpinner)

        builder.setPositiveButton("OK") { _, _ ->
            workoutMethod = workoutMethodSpinner.selectedItem.toString()
            textViewWorkoutMethod.text = workoutMethod
        }


        builder.setNegativeButton("Anuluj") { _, _ ->
            // Nic nie rób
        }

        builder.show()
    }



    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun calculateAge(birthDate: LocalDate?): String {
        if (birthDate != null) {
            val now = LocalDate.now()
            return "${now.year - birthDate.year}"
        }
        return "Wiek"
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }


    private fun selectImage() {
        val options = arrayOf<CharSequence>("Zrób zdjęcie", "Wybierz z galerii", "Anuluj")

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Dodaj zdjęcie")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Zrób zdjęcie" -> {
                    val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(takePicture, 0)
                }

                options[item] == "Wybierz z galerii" -> {
                    val pickPhoto =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(pickPhoto, 1)
                }

                options[item] == "Anuluj" -> {
                    dialog.dismiss()
                }
            }
        }
        builder.show()
    }


    override fun onBackPressed() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                0 -> {
                    val bitmap = data?.extras?.get("data") as Bitmap
                    profileImageUri = bitmapToUri(bitmap)  // Zaktualizuj profileImageUri tutaj
                    Glide.with(this)
                        .load(profileImageUri)
                        .circleCrop()
                        .into(profileImageView)
                }

                1 -> {
                    profileImageUri = data?.data  // Zaktualizuj profileImageUri tutaj
                    Glide.with(this)
                        .load(profileImageUri)
                        .circleCrop()
                        .into(profileImageView)
                }
            }
        }
    }


    class LatestExerciseInfo {
        var latestTimestamp: com.google.firebase.Timestamp? = null
        var latestWorkoutName: String? = null
    }


}
