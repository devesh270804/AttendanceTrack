package com.example.attendancetrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }

        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            val user = auth.currentUser
            if (user != null) {
                Toast.makeText(this, "Taking you to the next page", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ThirdActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Taking you to the sign-in page", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SecondActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, AttendanceActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}


