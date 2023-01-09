package com.example.espy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class LoginActivity : AppCompatActivity() {

    private lateinit var notHaveAccountLL: LinearLayout
    lateinit var etEmail: TextInputEditText
    private lateinit var etPass: TextInputEditText
    lateinit var btnLogin: Button

    // Creating firebaseAuth object
    lateinit var auth: FirebaseAuth

    override fun onStart() {
        if(auth.currentUser != null) {
            var intent = Intent(this@LoginActivity, DetectionActivity::class.java)
            startActivity(intent)
            finish()
        }
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // View Binding
        notHaveAccountLL = findViewById(R.id.ll_not_have_account)
        btnLogin = findViewById(R.id.btn_login)
        etEmail = findViewById(R.id.et_mail)
        etPass = findViewById(R.id.et_uid)

        // initialising Firebase auth object
        auth = FirebaseAuth.getInstance()

        btnLogin.setOnClickListener {
            login()
        }

        notHaveAccountLL.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // using finish() to end the activity
            finish()
        }
    }

    private fun login() {
        val email = etEmail.text?.trim().toString()
        val pass = etPass.text?.trim().toString()
        // calling signInWithEmailAndPassword(email, pass)
        // function using Firebase auth object
        // On successful response Display a Toast
        if(email.isEmpty() || !(Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            Toast.makeText(applicationContext, "Please enter valid email address", Toast.LENGTH_SHORT).show()
            return
        }else if(pass.isEmpty() || pass.length < 8) {
            Toast.makeText(applicationContext, "Please enter strong password", Toast.LENGTH_SHORT).show()
            return
        }
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Firebase.firestore.collection("Users").document(auth.currentUser!!.uid).get().addOnSuccessListener {
                    document ->

//                    Log.e("LOGIN", document.data?.get("email_address").toString())
                    // Storing data into SharedPreferences
                    val sharedPreferences = getSharedPreferences("SpyPreferences", MODE_PRIVATE)

                    // Creating an Editor object to edit(write to the file)
                    val myEdit = sharedPreferences.edit()

                    // Storing the key and its value as the data fetched from edittext
                    myEdit.putString("phone_number", document.data?.get("phone_number").toString())
                    myEdit.putInt("time_interval", document.data?.get("secondsIntervalBeforeScreenCapture").toString().toInt())
                    myEdit.putInt("first_capture_delay", document.data?.get("delayBetweenFirstCapture").toString().toInt())

                    myEdit.commit()

                    Toast.makeText(this, "Successfully LoggedIn", Toast.LENGTH_SHORT).show()
                    Log.e("LOGIN", auth.currentUser!!.uid)

                    startActivity(Intent(this, DetectionActivity::class.java))
                    finish()
                }
            } else
                Toast.makeText(this, "Log In failed " +  it.exception?.message, Toast.LENGTH_SHORT).show()
        }
    }

}