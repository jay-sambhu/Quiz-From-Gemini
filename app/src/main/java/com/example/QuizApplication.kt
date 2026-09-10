package com.example

import android.app.Application
import android.util.Log
import com.example.data.firestore.FirebaseAppCheckManager
import com.google.firebase.FirebaseApp

/**
 * Custom application class that bootstraps Firebase and initializes Firebase App Check
 * at the earliest possible stage in the app lifecycle.
 */
class QuizApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            FirebaseAppCheckManager.getInstance(this).initialize()
            Log.d("QuizApplication", "Firebase and App Check initialized on startup.")
        } catch (e: Exception) {
            Log.w("QuizApplication", "Startup notice: ${e.message}")
        }
    }
}
