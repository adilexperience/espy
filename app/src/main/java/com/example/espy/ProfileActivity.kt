package com.example.espy

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.NavUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ProfileActivity : AppCompatActivity() {
    lateinit var phoneNumberEt : EditText
    lateinit var secondsIntervalEt : EditText
    lateinit var delaySecondsEt : EditText
    lateinit var updateInformationCV : CardView
    lateinit var toolbar : Toolbar

    // variables to hold phone number and time interval
    lateinit var phoneNumber : String;
    lateinit var timeInterval : String;
    lateinit var delayBeforeFirstCapture : String;

    private fun getPreferencesData() {
        val sharedPreferences = getSharedPreferences("SpyPreferences", MODE_PRIVATE)
        // Creating an Editor object to edit(write to the file)

        // Creating an Editor object to edit(write to the file)
        phoneNumber = sharedPreferences.getString("phone_number", "").toString()
        timeInterval = sharedPreferences.getInt("time_interval", 0).toString()
        delayBeforeFirstCapture = sharedPreferences.getInt("first_capture_delay", 0).toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // getting already stored data from shared preferences
        getPreferencesData();

        // initializing ids with xml layout
        phoneNumberEt = findViewById(R.id.et_phone_number)
        secondsIntervalEt = findViewById(R.id.et_interval_seconds)
        delaySecondsEt = findViewById(R.id.et_delay_seconds)
        updateInformationCV = findViewById(R.id.cv_update_information)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        // adding pre-fetched data to fields
        phoneNumberEt.setText(phoneNumber)
        secondsIntervalEt.setText(timeInterval)
        delaySecondsEt.setText(delayBeforeFirstCapture)

        updateInformationCV.setOnClickListener(View.OnClickListener {
            processInformationAndUpdate()
        })
    }

    fun processInformationAndUpdate() {
        var _timeInterval : Int = secondsIntervalEt.text.trim().toString().toInt()
        var _delayInSeconds : Int = delaySecondsEt.text.trim().toString().toInt()
        var _phoneNumber : String = phoneNumberEt.text.trim().toString()

        if((_timeInterval.toString() == timeInterval) && (_delayInSeconds.toString() == delayBeforeFirstCapture) && ((_phoneNumber.length < 11) || (_phoneNumber == phoneNumber))) {
            Toast.makeText(applicationContext, "Enter new data to update, and phone number must 11 digits", Toast.LENGTH_SHORT).show()
            return
        }

        var db = Firebase.firestore.collection("Users").document(FirebaseAuth.getInstance().currentUser!!.uid)
        if(_phoneNumber.length >= 11 && (_phoneNumber != phoneNumber)) {
            // update value

            val newPhone = hashMapOf(
                "phone_number" to _phoneNumber
            )

            db.update(newPhone.toMap()).addOnSuccessListener {
                // updating shared preference value
                val sharedPreferences = getSharedPreferences("SpyPreferences", MODE_PRIVATE)
                val myEdit = sharedPreferences.edit()
                myEdit.putString("phone_number", _phoneNumber)
                myEdit.commit()

                // getting new value
                getPreferencesData()
                Toast.makeText(applicationContext, "Phone number updated", Toast.LENGTH_SHORT).show()
            }
        }
        if((_timeInterval.toString() != timeInterval)) {
            // update value
            val newPhone = hashMapOf(
                "secondsIntervalBeforeScreenCapture" to _timeInterval
            )

            db.update(newPhone.toMap()).addOnSuccessListener {
                // updating shared preference value
                val sharedPreferences = getSharedPreferences("SpyPreferences", MODE_PRIVATE)
                val myEdit = sharedPreferences.edit()
                myEdit.putInt("time_interval", _timeInterval)
                myEdit.commit()

                // getting new value
                getPreferencesData()
                Toast.makeText(applicationContext, "Time Interval updated", Toast.LENGTH_SHORT).show()
            }
        }

        if((_delayInSeconds.toString() != delayBeforeFirstCapture)) {
            // update value
            val newPhone = hashMapOf(
                "delayBetweenFirstCapture" to _delayInSeconds
            )

            db.update(newPhone.toMap()).addOnSuccessListener {
                // updating shared preference value
                val sharedPreferences = getSharedPreferences("SpyPreferences", MODE_PRIVATE)
                val myEdit = sharedPreferences.edit()
                myEdit.putInt("first_capture_delay", _delayInSeconds)
                myEdit.commit()

                // getting new value
                getPreferencesData()
                Toast.makeText(applicationContext, "Delay Interval updated", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Handle the home button being clicked
                finish()  // Finish this activity and navigate back to the main screen or home activity
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}