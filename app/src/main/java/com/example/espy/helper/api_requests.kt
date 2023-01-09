package com.example.espy.helper

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

class ApiRequests {
    companion object {

        fun logout() {
            FirebaseAuth.getInstance().signOut()
        }
    }
}