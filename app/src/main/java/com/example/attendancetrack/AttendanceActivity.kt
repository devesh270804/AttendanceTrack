package com.example.attendancetrack

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var subjectsList: List<String>
    private lateinit var checkBoxes: MutableMap<String, Triple<CheckBox, CheckBox, TextView>>
    private lateinit var submitButton: Button
    private lateinit var signOutButton: Button
    private lateinit var editSubjectsButton: Button
    private lateinit var attendanceLayout: LinearLayout
    private var currentDate: String = ""

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        submitButton = findViewById(R.id.submitButton)
        signOutButton = findViewById(R.id.signOutButton)
        editSubjectsButton=findViewById(R.id.editSubjectsButton)
        attendanceLayout = findViewById(R.id.attendanceLayout)
        checkBoxes = mutableMapOf()

        currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        loadSubjectsAndAttendance()

        submitButton.setOnClickListener {
            saveAttendance()
        }

        signOutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
            finish()
        }
        editSubjectsButton.setOnClickListener {
            val intent = Intent(this, ThirdActivity::class.java)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadSubjectsAndAttendance() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val subjects = document.get("subjects") as? List<*>
                        if (subjects != null && subjects.all { it is String }) {
                            subjectsList = subjects.filterIsInstance<String>()
                            displaySubjects()
                            loadAttendance(userId)
                        } else {
                            Log.w(TAG, "Subjects list is not in the expected format")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error getting document", e)
                }
        }
    }

    private fun displaySubjects() {
        subjectsList.forEach { subject ->
            val subjectRow = createSubjectRow(subject, currentDate)
            attendanceLayout.addView(subjectRow)
            checkBoxes[subject] = Triple(
                subjectRow.findViewById(R.id.classTakenCheckBox),
                subjectRow.findViewById(R.id.attendedCheckBox),
                subjectRow.findViewById(R.id.attendancePercentageTextView)
            )
        }
    }

    private fun createSubjectRow(subject: String, date: String): LinearLayout {
        val subjectRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        val subjectTextView = TextView(this).apply {
            text = subject
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val classTakenCheckBox = CheckBox(this).apply {
            id = R.id.classTakenCheckBox
            text = context.getString(R.string.class_taken2)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val attendedCheckBox = CheckBox(this).apply {
            id = R.id.attendedCheckBox
            text = context.getString(R.string.attended2)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            isEnabled = false
        }

        val attendancePercentageTextView = TextView(this).apply {
            id = R.id.attendancePercentageTextView
            text = "0%" // Default percentage
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        classTakenCheckBox.setOnCheckedChangeListener { _, isChecked ->
            attendedCheckBox.isEnabled = isChecked
            if (!isChecked) {
                attendedCheckBox.isChecked = false
            }
        }

        val dateTextView = TextView(this).apply {
            text = date
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        subjectRow.addView(subjectTextView)
        subjectRow.addView(classTakenCheckBox)
        subjectRow.addView(attendedCheckBox)
        subjectRow.addView(attendancePercentageTextView)
        subjectRow.addView(dateTextView)

        return subjectRow
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadAttendance(userId: String) {
        db.collection("attendance")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null) {
                    displayCurrentDayAttendance(documents)
                    calculateAttendancePercentages(documents)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting current day attendance", e)
            }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun calculateAttendancePercentages(documents: QuerySnapshot) {
        val attendanceCounts = mutableMapOf<String, Pair<Int, Int>>() // subject -> (classesTaken, classesAttended)

        for (document in documents) {
            val attendance = document.get("attendance") as? Map<*, *>
            if (attendance != null) {
                for ((subject, status) in attendance) {
                    if (subject is String && status is Map<*, *>) {
                        val classTaken = status["classTaken"] as? Boolean ?: false
                        val attended = status["attended"] as? Boolean ?: false
                        val counts = attendanceCounts.getOrDefault(subject, Pair(0, 0))
                        val updatedCounts = Pair(
                            counts.first + if (classTaken) 1 else 0,
                            counts.second + if (classTaken && attended) 1 else 0
                        )
                        attendanceCounts[subject] = updatedCounts
                    }
                }
            }
        }

        attendanceCounts.forEach { (subject, counts) ->
            val percentage = if (counts.first > 0) (counts.second.toDouble() / counts.first * 100).toInt() else 0
            checkBoxes[subject]?.third?.text = "$percentage%"
        }
    }

    private fun displayCurrentDayAttendance(documents: QuerySnapshot) {
        for (document in documents) {
            val attendance = document.get("attendance") as? Map<*, *>
            if (attendance != null) {
                for ((subject, status) in attendance) {
                    if (subject is String && status is Map<*, *>) {
                        val classTaken = status["classTaken"] as? Boolean ?: false
                        val attended = status["attended"] as? Boolean ?: false
                        val classTakenCheckBox = checkBoxes[subject]?.first
                        val attendedCheckBox = checkBoxes[subject]?.second
                        classTakenCheckBox?.isChecked = classTaken
                        attendedCheckBox?.isChecked = attended
                    }
                }
            }
        }
    }

    private fun saveAttendance() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val attendanceData = hashMapOf(
                "userId" to userId,
                "date" to date,
                "attendance" to checkBoxes.mapValues {
                    mapOf(
                        "classTaken" to it.value.first.isChecked,
                        "attended" to it.value.second.isChecked
                    )
                }
            )

            db.collection("attendance")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        db.collection("attendance").add(attendanceData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Attendance saved", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error adding document", e)
                            }
                    } else {
                        documents.forEach { document ->
                            db.collection("attendance").document(document.id).set(attendanceData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Attendance updated", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error updating document", e)
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error checking existing attendance", e)
                }
        }
    }
}
