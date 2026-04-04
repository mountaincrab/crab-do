package com.mountaincrab.crabdo

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mountaincrab.crabdo.BuildConfig
import com.mountaincrab.crabdo.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KanbanApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.USE_EMULATOR) {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        }

        NotificationHelper.createChannels(this)
    }
}
