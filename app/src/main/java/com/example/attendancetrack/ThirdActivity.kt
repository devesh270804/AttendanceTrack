package com.example.attendancetrack

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ThirdActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var subjectEditText: EditText
    private lateinit var addSubjectButton: Button
    private lateinit var submitButton: Button
    private lateinit var deleteSubjectButton: Button
    private lateinit var subjectsList: MutableList<String>
    private lateinit var subjectsAdapter: ArrayAdapter<String>
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)

        auth=FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


        subjectEditText = findViewById(R.id.subjectEditText)
        addSubjectButton = findViewById(R.id.addSubjectButton)
        submitButton = findViewById(R.id.submitButton)
        deleteSubjectButton = findViewById(R.id.deleteSubjectButton)
        listView = findViewById(R.id.subjectsListView)
        subjectsList = mutableListOf()
        subjectsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, subjectsList)
        listView.adapter = subjectsAdapter
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        loadSubjects()

        addSubjectButton.setOnClickListener {
            val subject = subjectEditText.text.toString()
            if (subject.isNotEmpty()) {
                subjectsList.add(subject)
                subjectsAdapter.notifyDataSetChanged()
                subjectEditText.text.clear()
            }
        }

        submitButton.setOnClickListener {
            saveSubjectsAndNavigate()
        }
        deleteSubjectButton.setOnClickListener {
            val position = listView.checkedItemPosition
            if (position != ListView.INVALID_POSITION) {
                subjectsList.removeAt(position)
                subjectsAdapter.notifyDataSetChanged()
                listView.clearChoices()
            } else {
                Toast.makeText(this, "Select a subject to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadSubjects() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val subjects = document.get("subjects") as? List<*>
                        if (subjects != null && subjects.all { it is String }) {
                            subjectsList.clear()
                            subjectsList.addAll(subjects.filterIsInstance<String>())
                            subjectsAdapter.notifyDataSetChanged()
                        } else {
                            Log.w(TAG, "Subjects list is not in the expected format")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error getting subjects", e)
                }
        }
    }

    private fun saveSubjectsAndNavigate() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userSubjects = hashMapOf("subjects" to subjectsList)
            db.collection("users").document(userId)
                .update(userSubjects as Map<String, Any>)
                .addOnSuccessListener {
                    val intent = Intent(this, AttendanceActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error updating document", e)
                }
        }
    }
}

