package com.mobicom.budgetwise

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.FirebaseApp

class LoginActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        FirebaseApp.initializeApp(this)

        val emailField = findViewById<EditText>(R.id.etvEmail)
        val passwordField = findViewById<EditText>(R.id.etvPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignup = findViewById<Button>(R.id.btnSignup)

        database = FirebaseDatabase.getInstance().reference.child("Users")

        btnLogin.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbRef = FirebaseDatabase.getInstance().reference.child("Users")
            dbRef.get().addOnSuccessListener { snapshot ->
                var foundUserId: String? = null
                for (userSnap in snapshot.children) {
                    val userEmail = userSnap.child("email").value as? String
                    val userPass = userSnap.child("password").value as? String
                    if (userEmail == email && userPass == password) {
                        foundUserId = userSnap.key
                        break
                    }
                }

                if (foundUserId != null) {
                    val sharedPrefs = getSharedPreferences("BudgetWisePrefs", MODE_PRIVATE)
                    with(sharedPrefs.edit()) {
                        putString("userId", foundUserId)
                        val email = emailField.text.toString()
                        putString("email", email)
                        apply()
                    }

                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("userId", foundUserId)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to login: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }


        btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
