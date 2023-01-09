package com.example.espy

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class NotifyUserAndAlertActivity : AppCompatActivity() {
    lateinit var detectedImage : ImageView
    lateinit var responseDetailsTV : TextView

    private val SEND_SMS_PERMISSION_REQUEST_CODE = 1

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notify_user_and_alert)

        // initializing view with id
        detectedImage = findViewById(R.id.iv_detected_image)
        responseDetailsTV = findViewById(R.id.tv_response_details)

        val extras = intent.extras
        val byteArray = extras!!.getByteArray("detected_image")

        responseDetailsTV.text =
            "Alert delivered to phone number ${getSharedPreferences("SpyPreferences", MODE_PRIVATE).getString("phone_number", "").toString()}. You are advised to take precautionary measures. If you need to access the images of captured suspect please reach out to system administrator at: mhassanawan47@gmail.com";
        val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
        detectedImage.setImageBitmap(bmp)

        // send sms to phone number from shared preferences
        if(hasSendSmsPermission()) {
            sendSMS(getSharedPreferences("SpyPreferences", MODE_PRIVATE).getString("phone_number", "").toString(), "Suspicious activity detected in device surrounding. Please be aware of possible threat. we saved the threat image in our records. If you need to access the images of captured suspect please reach out to system administrator at: mhassanawan47@gmail.com")
        }else {
            // request permission for sms
            requestSendSmsPermission(this@NotifyUserAndAlertActivity)
        }


        // upload image on firebase storage
        var storageRef = Firebase.storage.reference
        var imagesRef: StorageReference? = storageRef
            .child("Detection")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child(System.currentTimeMillis().toString())


        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 90, stream)
        val image = stream.toByteArray()
        var uploadTask = imagesRef?.putBytes(image)
        uploadTask?.addOnFailureListener {
            // Handle unsuccessful uploads
            exception -> Toast.makeText(applicationContext, exception.message, Toast.LENGTH_SHORT).show()
        }?.addOnCompleteListener { taskSnapshot ->
//            taskSnapshot.storage.path
            val db = Firebase.firestore
            var detectionReference = db.collection("Detection").document()

            // upload image path and uploader with time on fire-store
            imagesRef!!.downloadUrl.addOnSuccessListener { uri ->
                val detection = hashMapOf(
                    "id" to detectionReference.id,
                    "image_detected" to uri.toString(),
                    "user_id" to FirebaseAuth.getInstance().currentUser!!.uid,
                    "detected_at" to FieldValue.serverTimestamp()
                )
                detectionReference.set(detection).addOnSuccessListener {
                    Toast.makeText(this, "Possible threat saved in our database.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun requestSendSmsPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.SEND_SMS),
                SEND_SMS_PERMISSION_REQUEST_CODE)
        }
    }

    fun hasSendSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    fun sendSMS(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        Toast.makeText(this, "SMS sent at: $phoneNumber", Toast.LENGTH_SHORT).show()
    }
}