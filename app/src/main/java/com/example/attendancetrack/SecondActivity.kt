package com.example.attendancetrack


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class SecondActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var forgotPasswordTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        auth = Firebase.auth

        emailEditText= findViewById(R.id.emailEditText)
        passwordEditText= findViewById(R.id.passwordEditText)
        loginButton= findViewById(R.id.loginButton)
        signupButton = findViewById(R.id.signupButton)
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                createUser(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
        private fun loginUser(email: String, password: String) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val intent=Intent(this,AttendanceActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()

                    }
                }
        }

        private fun createUser(email: String, password: String) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val userData = hashMapOf(
                            "email" to email,
                            "password" to password // Do not store plain text passwords in production
                        )
                       val db=FirebaseFirestore.getInstance()
                        if(userId!=null){
                         db.collection("users").document(userId).set(userData)
                             .addOnSuccessListener {
                                 val intent=Intent(this,ThirdActivity::class.java)
                                 startActivity(intent)
                                 finish()
                             }
                             .addOnFailureListener { e->
                                 Log.w(TAG,"Error adding document",e)
                             }
                        }
                    } else {
                        Toast.makeText(
                            baseContext, "Registration failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    private fun signOut() {
        auth.signOut()

    }
    companion object {
            private const val TAG = "ThirdActivity"
        }
    }



