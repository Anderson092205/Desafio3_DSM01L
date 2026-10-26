package com.example.desafio3

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ReadingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val database = FirebaseDatabase.getInstance()
        if (BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            database.useEmulator("10.0.2.2", 9000)
        }
        database.setPersistenceEnabled(true)
    }
}
