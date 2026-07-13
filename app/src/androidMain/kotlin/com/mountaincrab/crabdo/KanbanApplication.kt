package com.mountaincrab.crabdo

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mountaincrab.crabdo.data.remote.AppSyncCoordinator
import com.mountaincrab.crabdo.di.appModule
import com.mountaincrab.crabdo.notification.NotificationHelper
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class KanbanApplication : Application() {

    private val syncCoordinator: AppSyncCoordinator by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@KanbanApplication)
            modules(appModule)
        }

        if (BuildConfig.USE_EMULATOR) {
            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
        }

        NotificationHelper.createChannels(this)

        // Start the real-time Firestore listeners (reminders + boards) at app scope,
        // driven by foreground state + sign-in. See AppSyncCoordinator.
        syncCoordinator.register()
    }
}
