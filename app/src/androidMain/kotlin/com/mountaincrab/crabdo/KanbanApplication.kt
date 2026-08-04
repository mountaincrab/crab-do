package com.mountaincrab.crabdo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mountaincrab.crabdo.data.remote.ForegroundSyncObserver
import com.mountaincrab.crabdo.di.appModule
import com.mountaincrab.crabdo.notification.NotificationHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class KanbanApplication : Application(), KoinComponent {

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

        // Pull remote changes every time the app returns to the foreground.
        ProcessLifecycleOwner.get().lifecycle.addObserver(get<ForegroundSyncObserver>())
    }
}
