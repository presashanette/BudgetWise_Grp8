package com.mobicom.budgetwise

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class EditPasswordActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var tvUserEmail: TextView
    private lateinit var etPassword: EditText
    private lateinit var etCPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var userId: String? = null
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_password)

        tvUserEmail = findViewById(R.id.tvUserEmail)
        etPassword = findViewById(R.id.etvPassword)
        etCPassword = findViewById(R.id.etvCPassword)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        val sharedPrefs = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE)
        val userId = sharedPrefs.getString("userId", null)
        val userEmail = sharedPrefs.getString("email", null)

//        userId = intent.getStringExtra("userId")
//        userEmail = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE).getString("email", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        tvUserEmail.text = userEmail ?: "Unknown email"
        database = FirebaseDatabase.getInstance().reference.child("Users").child(userId!!)

        btnSave.setOnClickListener {
            val newPassword = etPassword.text.toString().trim()
            val confirmPassword = etCPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update password in Firebase
            database.child("password").setValue(newPassword)
                .addOnSuccessListener {
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }
}
